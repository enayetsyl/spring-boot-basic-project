package dev.danvega.runners.run;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ui/runs")
public class RunPageController {

  private final RunRepository repo;
  private static final DateTimeFormatter FORM_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  public RunPageController(RunRepository repo) {
    this.repo = repo;
  }

  // List
  @GetMapping
  public String list(Model model) {
    model.addAttribute("runs", repo.findAll());
    return "runs-list";
  }

  // Show new form
  @GetMapping("/new")
  public String createForm(Model model) {
    model.addAttribute("formAction", "/ui/runs");
    model.addAttribute("methodOverride", "post");
    model.addAttribute("title", "");
    model.addAttribute("startedOn", "");
    model.addAttribute("completedOn", "");
    model.addAttribute("miles", "");
    model.addAttribute("location", "OUTDOOR");
    model.addAttribute("pageTitle", "Create Run");
    return "run-form";
  }

  // Handle create
  @PostMapping
  public String create(
      @RequestParam String title,
      @RequestParam(required = false) String startedOn,
      @RequestParam(required = false) String completedOn,
      @RequestParam Integer miles,
      @RequestParam Location location
  ) {
    LocalDateTime started = parseDt(startedOn);
    LocalDateTime completed = parseDt(completedOn);

    repo.save(new Run(null, title, started, completed, miles, location, null));
    return "redirect:/ui/runs";
  }

  // Show edit form
 @GetMapping("/{id}/edit")
public String editForm(@PathVariable Integer id, Model model) {
  var opt = repo.findById(id);
  if (opt.isEmpty()) return "redirect:/ui/runs";

  Run r = opt.get();
  model.addAttribute("formAction", "/ui/runs/" + r.id());
  model.addAttribute("methodOverride", "put");

  model.addAttribute("title", r.title());
  model.addAttribute("startedOn", formatDt(r.startedOn()));
  model.addAttribute("completedOn", formatDt(r.completedOn()));
  model.addAttribute("miles", r.miles());
  model.addAttribute("location", r.location().name());

  model.addAttribute("version", r.version());

  model.addAttribute("pageTitle", "Edit Run");
  return "run-form";
}


  // Handle update (via _method=put)
  @PostMapping("/{id}")
  public String update(
      @PathVariable Integer id,
      @RequestParam(required = false, name = "_method") String method,
      @RequestParam String title,
      @RequestParam(required = false) String startedOn,
      @RequestParam(required = false) String completedOn,
      @RequestParam Integer miles,
      @RequestParam Location location,
      @RequestParam(required = false) Integer version
  ) {
    if ("delete".equalsIgnoreCase(method)) {
      repo.deleteById(id);
      return "redirect:/ui/runs";
    }

    var started   = parseDt(startedOn);
  var completed = parseDt(completedOn);

  // ✅ if version not posted, fall back to DB version (safety)
  Integer v = (version != null)
      ? version
      : repo.findById(id).map(Run::version).orElse(null);

  repo.save(new Run(id, title, started, completed, miles, location, v));
  return "redirect:/ui/runs";
  }

  @PostMapping("/{id}/delete")
public String deletePost(@PathVariable Integer id) {
  repo.deleteById(id);
  return "redirect:/ui/runs";
}

  private static LocalDateTime parseDt(String s) {
    if (s == null || s.isBlank()) return null;
    return LocalDateTime.parse(s, FORM_FMT);
  }

  private static String formatDt(LocalDateTime dt) {
    return (dt == null) ? "" : dt.format(FORM_FMT);
    }
}
