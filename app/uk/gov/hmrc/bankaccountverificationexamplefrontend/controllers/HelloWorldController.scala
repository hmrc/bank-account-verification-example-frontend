package uk.gov.hmrc.bankaccountverificationexamplefrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.HelloWorldPage

import scala.concurrent.Future

@Singleton
class HelloWorldController @Inject()(
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  helloWorldPage: HelloWorldPage)
    extends FrontendController(mcc) {

  implicit val config: AppConfig = appConfig

  val helloWorld: Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(helloWorldPage()))
  }

}