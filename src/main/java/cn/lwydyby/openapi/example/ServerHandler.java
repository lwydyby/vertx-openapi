package cn.lwydyby.openapi.example;


import cn.lwydyby.openapi.annotation.VertxHandler;
import cn.lwydyby.openapi.annotation.VertxPath;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.vertx.ext.web.RoutingContext;

@VertxHandler
public interface ServerHandler {

    @Operation(summary = "Creates a new upload unit-of-work.", method = "POST",
            parameters = {@Parameter(name = "Upload-Length", in = ParameterIn.HEADER, required = true),
                    @Parameter(name = "Upload-Concat", in = ParameterIn.HEADER, schema = @Schema(type = "string")),
                    @Parameter(name = "Upload-Metadata", in = ParameterIn.HEADER, schema = @Schema(type = "string"))},
            responses = {
                    @ApiResponse(responseCode = "413", description = "Upload size too large."),
                    @ApiResponse(responseCode = "400", description = "Bad Request."),
                    @ApiResponse(responseCode = "201", description = "Upload unit of work Created.",
                            headers = {@Header(name = "Location", description = "The uri of the created upload unit of work.", required = true)})})
    @VertxPath(path = "/servers")
    void testHandler(RoutingContext context);
}
