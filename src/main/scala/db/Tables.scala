package db

import model._
import slick.jdbc.MySQLProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class UserTable(tag: Tag)
  extends Table[User](tag,"Users") {

  def id:Rep[Int] = column[Int]("USER_ID", O.PrimaryKey) // This is the primary key column
  def name:Rep[String] = column[String]("USER_NAME")
  def email:Rep[String] = column[String]("USER_EMAIL")

  override def * : ProvenShape[User] = (id, name, email) <> (User.tupled, User.unapply)
}

class RequestTable(tag: Tag)
  extends Table[Request](tag,"Requests") {

  def requestID = column[Int]("REQUEST_ID", O.PrimaryKey) // This is the primary key column
  def userID = column[Int]("USER_ID") // TODO: make it a foreign key
  def repoName = column[String]("REPO_NAME")

  override def * = (requestID, userID, repoName) <> (Request.tupled, Request.unapply)
}

class ResponseTable(tag: Tag)
  extends Table[Response](tag,"Responses") {

  def requestID = column[Int]("REQUEST_ID") // TODO: make it a foreign key
  def status = column[String]("USER_NAME")

  override def * = (requestID, status) <> (Response.tupled, Response.unapply)
}

class UserDb (db: MySQLProfile.backend.DatabaseDef)(implicit ec: ExecutionContext)
  extends TableQuery(new UserTable(_)){

  def insert(user: User) = {
    val setup = DBIO.seq(
      this.schema.createIfNotExists,
      this += user
    )
  db.run(setup)
  }

  def getUsers() ={
    db.run(this.result)
  }
}

object Main extends App {

  val dbConnection = new DBConnection

  val userDb = new UserDb(dbConnection.db)

  val insertFuture = userDb.insert(User(14,"Prakash","pr@gmail.com"))

  Await.result(insertFuture, Duration.Inf)

  val usersFuture = userDb.getUsers()
  usersFuture.map(_.foreach {
    case User(id, name, email) =>
      println(s"$id, $name, $email")
  })

  Thread.sleep(2000)
}


/*
object Tables {
  val db = Database.forConfig("h2db")

  val users = TableQuery[UserTable]

  val setup = DBIO.seq(
    users.schema.create,
    users ++= Seq(
      User(1,"Prateek", "pkgp@gmail.com"),
      User(2,"Parul", "parul@gmail.com"),
      User(3,"Pasksrul", "passnrul@gmail.com")
    )
  )
  val setupFuture = db.run(setup)
  Await.result(setupFuture, Duration.Inf)

/*  println("Users: ")
  val usersFuture = db.run(users.result)
  usersFuture.map(_.foreach{
  case User(id, name, email) =>
    println(s"$id, $name, $email")
  })*/

  val userFilterQuery = users.filter(_.id === 2)
  val userFilterQueryFuture:Future[Seq[User]] = db.run[Seq[User]](userFilterQuery.result)
  userFilterQueryFuture.onComplete{
    case Success(value) => println("result: " + value.head)
    case Failure(exception) => println(s"F2 $exception")
  }

  Await.result(userFilterQueryFuture, Duration.Inf)
}*/
