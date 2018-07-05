package example.micronaut

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.Client

@CompileStatic
@Client("/")
interface AppClient {

    @Get("/")
    String home(@Header String authorization)
}