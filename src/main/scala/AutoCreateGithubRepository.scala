import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success}
import spray.json._

import model._

trait GithubRepoJSONProtocol extends DefaultJsonProtocol{
  implicit val githubRepoFormat: RootJsonFormat[Repository] = jsonFormat1(Repository)
}

object AutoCreateGithubRepository extends App with GithubRepoJSONProtocol {
  implicit val actorSystem = ActorSystem("AutoCreateGithubRepository")
  implicit val materializer = ActorMaterializer()

  val accessToken = sys.env("token")

  val authHeaderWithToken = Authorization(OAuth2BearerToken(accessToken))

  val createRepoRoute =
    pathPrefix("api") {
      post {
          (path("createRepo" / Segment) & extractLog) { (repoName, log) =>
            log.info(s"Got one request to create a new github repo by the name: $repoName")
            val newGithubRepo = Repository(repoName)
            val createRepoResponseFuture = Http().singleRequest(
              HttpRequest(
                HttpMethods.POST,
                uri = "https://api.github.com/user/repos",
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  newGithubRepo.toJson.prettyPrint
                ),
                headers = List(authHeaderWithToken)
              )
            )
            onComplete(createRepoResponseFuture) {
              case Success(response) =>
                response.discardEntityBytes()
                println(s"The request was successful and returned: $response")
                println(s"Status Code: ${response.status}")
                complete(response.status)
              case Failure(exception) =>
                failWith(exception)
            }
          }
        }
    }

  Http().bindAndHandle(createRepoRoute, "localhost", 8080)
}
