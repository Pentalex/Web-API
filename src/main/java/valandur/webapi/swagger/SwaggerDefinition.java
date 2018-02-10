package valandur.webapi.swagger;

import io.swagger.annotations.*;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Model;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import valandur.webapi.WebAPI;
import valandur.webapi.api.servlet.BaseServlet;
import valandur.webapi.util.Constants;

import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

@io.swagger.annotations.SwaggerDefinition(
        info = @Info(
                title = Constants.NAME,
                version = Constants.VERSION,
                description = "Access Sponge powered Minecraft servers through a WebAPI\n\n" +
                        "# Introduction\n" +
                        "This is the documentation of the various API routes offered by the WebAPI plugin.\n\n" +
                        "This documentation assumes that you are familiar with the basic concepts of Web API's, " +
                        "such as `GET`, `PUT`, `POST` and `DELETE` methods, request `HEADERS` and `RESPONSE CODES` " +
                        "and `JSON` data.\n\n" +
                        "By default this documentation can be found at http:/localhost:8080 " +
                        "(while your minecraft server is running) and the various routes start with " +
                        "http:/localhost:8080/api/v5...\n\n" +
                        "As a quick test try reaching the route http:/localhost:8080/api/v5/info " +
                        "(remember that you can only access \\\"localhost\\\" routes on the server on which you " +
                        "are running minecraft).\n" +
                        "This route should show you basic information about your server, like the motd and " +
                        "player count.\n\n" +
                        "# List endpoints\n" +
                        "Lots of objects offer an endpoint to list all objects (e.g. `GET: /world` to get all worlds). " +
                        "These endpoints return only the properties marked 'required' by default, because the list " +
                        "might be quite large. If you want to return ALL data for a list endpoint add the query " +
                        "parameter `details`, (e.g. `GET: /world?details`).\n\n" +
                        "> Remember that in this case the data returned by the endpoint might be quite large.\n\n" +
                        "# Additional data\n" +
                        "Certain endpoints (such as `/player`, `/entity` and `/tile-entity` have additional " +
                        "properties which are not documented here, because the data depends on the concrete " +
                        "object type (eg. `Sheep` have a wool color, others do not) and on the other plugins/mods " +
                        "that are running on your server which might add additional data.\n\n" +
                        "You can also find more information in the github docs " +
                        "(https:/github.com/Valandur/Web-API/tree/master/docs/DATA.md)",
                contact = @Contact(
                        name = "Valandur",
                        email = "inithilian@gmail.com",
                        url = "https://github.com/Valandur"
                ),
                license = @License(
                        name = "MIT",
                        url = "https://github.com/Valandur/Web-API/blob/master/LICENSE"
                )
        ),
        consumes = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML },
        produces = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML },
        securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
                @ApiKeyAuthDefinition(
                        key = "ApiKeyHeader",
                        name = "X-WebAPI-Key",
                        description = "Authorize using an HTTP header. This can also be done using the " +
                                "`Authorization` header with a `Bearer` token",
                        in = ApiKeyAuthDefinition.ApiKeyLocation.HEADER),
                @ApiKeyAuthDefinition(
                        key = "ApiKeyQuery",
                        name = "key",
                        description = "Authorize using a query value.",
                        in = ApiKeyAuthDefinition.ApiKeyLocation.QUERY),
        })
)
public class SwaggerDefinition implements ReaderListener {

    @Override
    public void beforeScan(Reader reader, Swagger swagger) {
    }

    @Override
    public void afterScan(Reader reader, Swagger swagger) {
        List<String> webapiTags = new ArrayList<>();
        List<String> integrationTags = new ArrayList<>();

        swagger.setTags(new ArrayList<>());

        // Collect tags for servlets
        for (Class<? extends BaseServlet> servletClass : WebAPI.getServletService().getRegisteredServlets().values()) {
            Api api = servletClass.getAnnotation(Api.class);
            String descr = api.value();

            Set<String> tags = new HashSet<>(Arrays.asList(api.tags()));
            tags.addAll(Arrays.stream(servletClass.getMethods())
                    .flatMap(m -> Arrays.stream(m.getAnnotationsByType(ApiOperation.class)))
                    .flatMap(a -> Arrays.stream(a.tags()))
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList()));

            if (servletClass.getPackage().getName().startsWith("valandur.webapi.servlet")) {
                for (String tag : tags) {
                    webapiTags.add(tag);
                    swagger.addTag(new io.swagger.models.Tag().name(tag).description(descr));
                }
            } else {
                for (String tag : tags) {
                    integrationTags.add(tag);
                    swagger.addTag(new io.swagger.models.Tag().name(tag).description(descr));
                }
            }
        }

        // Sort properties by "required" and alphabetically
        for (Model model : swagger.getDefinitions().values()) {
            Map<String, Property> props = new LinkedHashMap<>();
            if (model.getProperties() == null) continue;

            List<Map.Entry<String, Property>> newProps = model.getProperties().entrySet().stream()
                    .sorted((p1, p2) -> {
                        int req = Boolean.compare(p2.getValue().getRequired(), p1.getValue().getRequired());
                        if (req != 0) return req;
                        return p1.getKey().compareTo(p2.getKey());
                    }).collect(Collectors.toList());
            for (Map.Entry<String, Property> newProp : newProps) {
                props.put(newProp.getKey(), newProp.getValue());
            }
            model.getProperties().clear();
            model.setProperties(props);
        }

        // Sort tags alphabetically
        webapiTags.sort(String::compareTo);
        integrationTags.sort(String::compareTo);

        // Sort paths alphabetically
        List<Map.Entry<String, Path>> paths = swagger.getPaths().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .collect(Collectors.toList());
        swagger.setPaths(new LinkedHashMap<>());
        for (Map.Entry<String, Path> entry : paths) {
            swagger.path(entry.getKey(), entry.getValue());
        }

        // Add tag groups for redoc
        swagger.vendorExtension("x-tagGroups", Arrays.asList(
                new TagGroup("Web-API", webapiTags),
                new TagGroup("Integrations", integrationTags)
        ));
    }


    public static class TagGroup {
        private String name;
        public String getName() {
            return name;
        }

        private List<String> tags;
        public List<String> getTags() {
            return tags;
        }

        public TagGroup(String name, List<String> tags) {
            this.name = name;
            this.tags = tags;
        }
    }
}
