<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
