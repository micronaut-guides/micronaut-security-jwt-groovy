package example.micronaut

import com.nimbusds.jwt.JWTParser
import com.nimbusds.jwt.SignedJWT
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.security.authentication.UsernamePasswordCredentials
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken
import io.micronaut.test.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest // <1>
class JwtAuthenticationSpec extends Specification {

    @Inject
    EmbeddedServer embeddedServer // <2>

    @Inject
    @Client("/")
    RxHttpClient client // <3>

    def "Verify JWT authentication works"() {
        when: 'Accessing a secured URL without authenticating'
        client.toBlocking().exchange(HttpRequest.GET('/', )) // <4>

        then: 'returns unauthorized'
        HttpClientResponseException e = thrown(HttpClientResponseException)
        e.status == HttpStatus.UNAUTHORIZED

        when: 'Login endpoint is called with valid credentials'
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials("sherlock", "password")
        HttpRequest request = HttpRequest.POST('/login', creds) // <5>
        HttpResponse<BearerAccessRefreshToken> rsp = client.toBlocking().exchange(request, BearerAccessRefreshToken) // <6>

        then: 'the endpoint can be accessed'
        rsp.status == HttpStatus.OK
        rsp.body().username == 'sherlock'
        rsp.body().accessToken
        JWTParser.parse(rsp.body().accessToken) instanceof SignedJWT
        rsp.body().refreshToken
        JWTParser.parse(rsp.body().refreshToken) instanceof SignedJWT

        when:
        String accessToken = rsp.body().accessToken
        HttpRequest requestWithAuthorization = HttpRequest.GET('/' ).header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken") // <7>
        HttpResponse<String> response = client.toBlocking().exchange(requestWithAuthorization, String)

        then:
        response.status == HttpStatus.OK
        response.body() == 'sherlock' // <8>
    }
}
