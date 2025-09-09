
# Run Tracking — JSP Frontend Guide (Beginner Friendly)

This document explains how the **JSP-based frontend** is wired on top of your Spring Boot backend. We’ll go step-by-step and explain the purpose of **every dependency, property, annotation, tag**, and key line of code.

---

## Step 1 — Dependencies (what and why)

Add these **frontend** dependencies to your `pom.xml` (in addition to your existing Spring Boot dependencies such as Web, Data JDBC, H2, Validation, DevTools).

```xml
<!-- JSP compilation/runtime for embedded Tomcat (Boot 3 uses Tomcat 10) -->
<dependency>
  <groupId>org.apache.tomcat.embed</groupId>
  <artifactId>tomcat-embed-jasper</artifactId>
</dependency>

<!-- JSTL (Jakarta) — standard tag library for JSP (e.g., <c:forEach>) -->
<dependency>
  <groupId>jakarta.servlet.jsp.jstl</groupId>
  <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
</dependency>
<dependency>
  <groupId>org.glassfish.web</groupId>
  <artifactId>jakarta.servlet.jsp.jstl</artifactId>
</dependency>
```

**Why these?**  
- **`tomcat-embed-jasper`** — adds JSP support to the embedded Tomcat that Spring Boot uses. Without it, `.jsp` files won’t be compiled/rendered.  
- **JSTL (API + implementation)** — gives you standard tags like `<c:forEach>` for loops and conditionals in JSP pages. The **Jakarta** versions are required for Spring Boot 3 (which uses `jakarta.*` packages).

> You already have the rest (Spring Web, Data JDBC, H2, Validation, DevTools). Those power your controllers, database access, in-memory DB, form validation, and auto-restart during development.

---

## Step 2 — `application.properties` configuration (where views live + H2 console)

```properties
# (Optional) App display name
spring.application.name=runners

# Stable in-memory DB name so the JDBC URL is predictable (jdbc:h2:mem:runners)
spring.datasource.generate-unique-name=false
spring.datasource.name=runners

# Enable the H2 web console so you can see tables/rows in the browser
spring.h2.console.enabled=true
# spring.h2.console.path=/h2-console  # default is /h2-console

# Always run schema.sql to create tables (handy for dev)
spring.sql.init.mode=always

# Tell Spring MVC where your JSP files are and what suffix they use
spring.mvc.view.prefix=/WEB-INF/views/
spring.mvc.view.suffix=.jsp
```

**Beginner notes:**  
- **JSP location** — we put JSPs under `src/main/webapp/WEB-INF/views/`. They are **not** directly accessible by URL (that’s good). You reach them by returning a **view name** (e.g., `"runs-list"`) from a `@Controller`.  
- **H2 console** — open `http://localhost:8080/h2-console`, use `jdbc:h2:mem:runners`, user `sa`, blank password.  
- **`spring.sql.init.mode=always`** — ensures `schema.sql` runs even if the DB was already created in this run; it helps in dev to make sure the table exists.

---

## Step 3 — Run Page Controller (server-rendered pages)

**File:** `src/main/java/dev/danvega/runners/run/RunPageController.java`

```java
package dev.danvega.runners.run;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller                          // ① A web MVC controller (returns view names, not JSON)
@RequestMapping("/ui/runs")          // ② Base URL path for all page routes
public class RunPageController {

  private final RunRepository repo;
  private static final DateTimeFormatter FORM_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"); // ③ matches <input type="datetime-local">

  public RunPageController(RunRepository repo) {         // ④ Constructor Injection (Spring provides repo)
    this.repo = repo;
  }

  // ---------- READ (List) ----------
  @GetMapping                        // ⑤ Handles GET /ui/runs
  public String list(Model model) {
    model.addAttribute("runs", repo.findAll());          // ⑥ Put data into the model for the JSP
    return "runs-list";                                  // ⑦ Render /WEB-INF/views/runs-list.jsp
  }

  // ---------- CREATE (Form + Submit) ----------
  @GetMapping("/new")                                      // ⑧ Show the empty form
  public String createForm(Model model) {
    model.addAttribute("formAction", "/ui/runs");          // ⑨ Form posts to POST /ui/runs
    model.addAttribute("methodOverride", "post");          // (optional, not required)
    model.addAttribute("title", "");
    model.addAttribute("startedOn", "");
    model.addAttribute("completedOn", "");
    model.addAttribute("miles", "");
    model.addAttribute("location", "OUTDOOR");
    model.addAttribute("pageTitle", "Create Run");
    return "run-form";
  }

  @PostMapping                                            // ⑩ Handle form submission (create)
  public String create(
      @RequestParam String title,                         // ⑪ Reads input fields by 'name' attribute
      @RequestParam(required = false) String startedOn,
      @RequestParam(required = false) String completedOn,
      @RequestParam Integer miles,
      @RequestParam Location location
  ) {
    LocalDateTime started = parseDt(startedOn);           // ⑫ Convert HTML datetime-local to LocalDateTime
    LocalDateTime completed = parseDt(completedOn);

    repo.save(new Run(null, title, started, completed, miles, location, null)); // ⑬ INSERT (version null)
    return "redirect:/ui/runs";                           // ⑭ PRG pattern: redirect to list
  }

  // ---------- UPDATE (Form + Submit) ----------
  @GetMapping("/{id}/edit")                               // ⑮ Load the form pre-filled with existing values
  public String editForm(@PathVariable Integer id, Model model) {
    var opt = repo.findById(id);
    if (opt.isEmpty()) return "redirect:/ui/runs";

    Run r = opt.get();
    model.addAttribute("formAction", "/ui/runs/" + r.id());
    model.addAttribute("methodOverride", "put");          // (optional hint; we use POST endpoint below)

    model.addAttribute("title", r.title());
    model.addAttribute("startedOn", formatDt(r.startedOn()));
    model.addAttribute("completedOn", formatDt(r.completedOn()));
    model.addAttribute("miles", r.miles());
    model.addAttribute("location", r.location().name());
    model.addAttribute("version", r.version());           // ⑯ include @Version for optimistic locking
    model.addAttribute("pageTitle", "Edit Run");
    return "run-form";
  }

  @PostMapping("/{id}")                                  // ⑰ Handle the edit submission (we use POST)
  public String update(
      @PathVariable Integer id,                          // ⑱ Path piece from URL /ui/runs/{id}
      @RequestParam(required = false, name = "_method") String method,
      @RequestParam String title,
      @RequestParam(required = false) String startedOn,
      @RequestParam(required = false) String completedOn,
      @RequestParam Integer miles,
      @RequestParam Location location,
      @RequestParam(required = false) Integer version     // ⑲ carries the current row version
  ) {
    var started   = parseDt(startedOn);
    var completed = parseDt(completedOn);

    // ⑳ If the form didn't send version, read it from DB; otherwise use the posted one
    Integer v = (version != null) ? version : repo.findById(id).map(Run::version).orElse(null);

    repo.save(new Run(id, title, started, completed, miles, location, v)); // UPDATE (version != null)
    return "redirect:/ui/runs";
  }

  // ---------- DELETE (Submit) ----------
  @PostMapping("/{id}/delete")                             // ㉑ Separate POST endpoint for deletions
  public String deletePost(@PathVariable Integer id) {
    repo.deleteById(id);
    return "redirect:/ui/runs";
  }

  // ---------- Helpers ----------
  private static LocalDateTime parseDt(String s) {
    if (s == null || s.isBlank()) return null;
    return LocalDateTime.parse(s, FORM_FMT);
  }
  private static String formatDt(LocalDateTime dt) {
    return (dt == null) ? "" : dt.format(FORM_FMT);
  }
}
```

**What each piece means (short explanations):**  
1. **`@Controller`**: Spring MVC component that returns **view names** (for JSP).  
2. **`@RequestMapping("/ui/runs")`**: Base URL so all methods are under `/ui/runs`.  
3. **`DateTimeFormatter`**: Pattern `yyyy-MM-dd'T'HH:mm` matches HTML `<input type="datetime-local">`.  
4. **Constructor injection**: Spring gives you a `RunRepository`.  
5. **`@GetMapping`**: Handles a HTTP **GET**.  
6. **`Model`**: A bag of attributes passed to the JSP.  
7. **Return `"runs-list"`**: Spring resolves `/WEB-INF/views/runs-list.jsp`.  
8–10. **Create flow**: One method to show the **empty form**, another to **handle POST**.  
11–14. **`@RequestParam`** grabs form fields by name; we save and redirect.  
15–19. **Edit flow**: Pre-fill the form with current values; include the **version** for optimistic locking.  
17. We **POST to `/{id}`** for simplicity (no need for `_method=PUT`).  
20. **Version logic**: `version == null` → insert, otherwise update.  
21. **Separate delete endpoint**: Simple form that posts to `/{id}/delete`.

---

## Step 4 — `runs-list.jsp` (table of runs with Edit/Delete)

**Location:** `src/main/webapp/WEB-INF/views/runs-list.jsp`

```jsp
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>  <!-- JSTL core tag library (Jakarta) -->
<!doctype html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Runs</title>
  <style>
    body { font-family: system-ui, Arial, sans-serif; margin: 2rem; }
    table { border-collapse: collapse; width: 100%; }
    th, td { border: 1px solid #ddd; padding: .5rem .75rem; }
    th { background: #f6f6f6; text-align: left; }
    .actions { display: flex; gap: .5rem; }
    a.button, button { padding: .4rem .7rem; border: 1px solid #ccc; background: #fafafa; cursor: pointer; }
    a.button { text-decoration: none; color: inherit; }
  </style>
</head>
<body>

  <h1>Runs</h1>

  <p>
    <a class="button" href="${pageContext.request.contextPath}/ui/runs/new">+ New Run</a>
  </p>

  <table>
    <thead>
      <tr>
        <th>ID</th>
        <th>Title</th>
        <th>Started</th>
        <th>Completed</th>
        <th>Miles</th>
        <th>Location</th>
        <th>Actions</th>
      </tr>
    </thead>
    <tbody>
    <c:forEach items="${runs}" var="r">
      <tr>
        <!-- EL with Java Records: call accessors like id(), title(), etc. -->
        <td>${r.id()}</td>
        <td>${r.title()}</td>
        <td>${r.startedOn()}</td>
        <td>${r.completedOn()}</td>
        <td>${r.miles()}</td>
        <td>${r.location()}</td>
        <td class="actions">
          <a class="button" href="${pageContext.request.contextPath}/ui/runs/${r.id()}/edit">Edit</a>
          <form action="${pageContext.request.contextPath}/ui/runs/${r.id()}/delete" method="post" style="display:inline">
             <button type="submit" onclick="return confirm('Delete this run?')">Delete</button>
          </form>
        </td>
      </tr>
    </c:forEach>
    </tbody>
  </table>

</body>
</html>
```

**What’s happening:**  
- **Page directive**: sets the content type to UTF-8.  
- **Taglib line**: enables JSTL **core** tags with prefix `c`. (Boot 3 / Tomcat 10 use `jakarta.tags.core`; older apps used `http://java.sun.com/jsp/jstl/core`.)  
- **`${pageContext.request.contextPath}`**: adds the app’s context path (useful if not running at `/`).  
- **`<c:forEach items="${runs}" var="r">`**: loops over the list from the model attribute `runs`.  
- **Records in EL**: For Java **records**, use `${r.id()}` **not** `${r.id}` (that pattern expects a `getId()` bean getter).

---

## Step 5 — `run-form.jsp` (create/edit form)
> You mentioned “run-form.jsx” — since this is a JSP frontend, the file is **`run-form.jsp`**.

**Location:** `src/main/webapp/WEB-INF/views/run-form.jsp`

```jsp
<%@ page contentType="text/html; charset=UTF-8" %>
<!doctype html>
<html>
<head>
  <meta charset="UTF-8">
  <title>${pageTitle}</title>
  <style>
    body { font-family: system-ui, Arial, sans-serif; margin: 2rem; max-width: 720px; }
    label { display: block; margin-top: .75rem; font-weight: 600; }
    input, select { width: 100%; padding: .45rem .6rem; margin-top: .25rem; }
    .row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .actions { margin-top: 1rem; display: flex; gap: .5rem; }
    a.button, button { padding: .45rem .7rem; border: 1px solid #ccc; background: #fafafa; text-decoration: none; color: inherit; cursor: pointer; }
  </style>
</head>
<body>

  <h1>${pageTitle}</h1>

  <!-- The 'action' and the field values are injected by the controller -->
  <form action="${formAction}" method="post">
    <!-- Optional leftover from method-override patterns (not required because we POST to /{id}) -->
    <input type="hidden" name="_method" value="${methodOverride}" />

    <label>Title
      <input type="text" name="title" value="${title}" required />
    </label>

    <div class="row">
      <div>
        <label>Started On
          <!-- HTML datetime-local must be yyyy-MM-dd'T'HH:mm; controller formats it -->
          <input type="datetime-local" name="startedOn" value="${startedOn}" />
        </label>
      </div>
      <div>
        <label>Completed On
          <input type="datetime-local" name="completedOn" value="${completedOn}" />
        </label>
      </div>
    </div>

    <div class="row">
      <div>
        <label>Miles
          <input type="number" name="miles" min="1" step="1" value="${miles}" required />
        </label>
      </div>
      <div>
        <label>Location
          <select name="location" required>
            <option value="INDOOR" ${location == 'INDOOR' ? 'selected' : ''}>INDOOR</option>
            <option value="OUTDOOR" ${location == 'OUTDOOR' ? 'selected' : ''}>OUTDOOR</option>
          </select>
        </label>
      </div>

      <!-- Important for Spring Data JDBC optimistic locking -->
      <input type="hidden" name="version" value="${version}" />
    </div>

    <div class="actions">
      <button type="submit">Save</button>
      <a class="button" href="${pageContext.request.contextPath}/ui/runs">Cancel</a>
    </div>
  </form>

</body>
</html>
```

**Key explanations:**  
- **`action="${formAction}"`** is set by the controller to either `/ui/runs` (create) or `/ui/runs/{id}` (edit).  
- **`name="..."`** attributes (e.g., `title`, `miles`) must match the `@RequestParam` names in the controller.  
- **`type="datetime-local"`** inputs require a specific string format; the controller formats/parses with `DateTimeFormatter`.  
- **Hidden `version`**: preserves the current row’s `@Version` so **Spring Data JDBC** performs an **UPDATE** instead of an INSERT and enforces optimistic locking.

---

## Step 6 — Testing the UI

1. **Start the app**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Open the list page**  
   Visit **`http://localhost:8080/ui/runs`**. If the table is empty, click **“+ New Run”**.

3. **Create a run**  
   - Fill **Title**, **Miles**, optionally **Started/Completed** and **Location**.  
   - Click **Save** → you’ll be redirected to the list. The new row appears.

4. **Edit a run**  
   - Click **Edit** on any row.  
   - Change fields and **Save**. The hidden **`version`** comes along so updates are safe.

5. **Delete a run**  
   - Click **Delete** on a row and confirm. It posts to `/ui/runs/{id}/delete` and returns to the list.

6. **Common pitfalls**  
   - **400 “Required parameter is not present”** → a form posted to the wrong URL or missing `name="..."` fields. Ensure form field names match the `@RequestParam` names.  
   - **Primary key duplicate on update** → usually means the form didn’t send **`version`** (or you dropped it in the controller). Make sure the hidden `version` field is present on the edit form and the controller passes it to `new Run(..., version)`.

---

