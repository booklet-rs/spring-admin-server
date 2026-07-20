# Local Development

## Rebuild the admin UI

The Vue UI is packaged as a Maven dependency. A Vite build alone only updates
`spring-admin-admin-ui/target/dist` and is not enough for the server to use the
new bundle.

Build the UI JAR and install it into the local Maven repository:

```bash
./mvnw -pl spring-admin-admin-ui install -DskipTests
```

## Run the server with the rebuilt UI

Stop any server already listening on port 9000, then run:

```bash
./dev-server
```

This installs the latest UI JAR and starts the Spring application. The server
loads the UI from the installed Maven dependency, so UI changes require another
install and server restart. The UI does not hot reload.

## Disable browser caching

The browser can continue using an old `custom-ui.js` after the server restarts.
During UI development, open the browser developer tools, select the Network
tab, enable **Disable cache**, and keep developer tools open. Use
`Ctrl+Shift+R` to force a reload after restarting the server.
