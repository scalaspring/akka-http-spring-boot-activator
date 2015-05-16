package sample.combo

import com.github.scalaspring.akka.http.{AkkaHttpServer, AkkaHttpServerAutoConfiguration}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import sample.quote.BollingerQuoteService
import sample.random.RandomUserDataService

@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends AkkaHttpServer with BollingerQuoteService with RandomUserDataService

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
