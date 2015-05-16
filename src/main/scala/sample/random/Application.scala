package sample.random

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.scalaspring.akka.http.{AkkaHttpServer, AkkaHttpServerAutoConfiguration, AkkaHttpService}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import


trait RandomUserDataService extends AkkaHttpService with JsonProtocols {

  abstract override def route = {
    get {
      pathPrefix("random") {
        path("name") {
          complete(Name.random)
        } ~
        path("creditcard") {
          complete(CreditCard.random)
        } ~
        path("user") {
          complete(User.random)
        }
      }
    }
  } ~ super.route

}


@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends AkkaHttpServer with RandomUserDataService {}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
