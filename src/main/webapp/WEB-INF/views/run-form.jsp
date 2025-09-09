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

  <form action="${formAction}" method="post">
    <input type="hidden" name="_method" value="${methodOverride}" />

    <label>Title
      <input type="text" name="title" value="${title}" required />
    </label>

    <div class="row">
      <div>
        <label>Started On
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
      <div>
        <input type="hidden" name="version" value="${version}" />

      </div>
    </div>

    <div class="actions">
      <button type="submit">Save</button>
      <a class="button" href="${pageContext.request.contextPath}/ui/runs">Cancel</a>
    </div>
  </form>

</body>
</html>
