/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bankaccountverificationexamplefrontend.example

import play.api.data.Form

import javax.inject.{Inject, Singleton}
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.example.html.PetDetails
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html.{BusinessDonePage, CheckYourBusinessAnswersPage, CheckYourPersonalAnswersPage, MorePetDetailsPage, PersonalDonePage}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.{AuthProviderId, BavfConnector, BusinessCompleteResponse, CompleteResponse, InitRequestMessages, InitRequestPrepopulatedData, PersonalCompleteResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class MakingPetsDigitalController @Inject()(appConfig: AppConfig,
                                            connector: BavfConnector,
                                            val authConnector: AuthConnector,
                                            mcc: MessagesControllerComponents,
                                            petDetails: PetDetails,
                                            morePetDetails: MorePetDetailsPage,
                                            checkYourBusinessAnswersPage: CheckYourBusinessAnswersPage,
                                            checkYourPersonalAnswersPage: CheckYourPersonalAnswersPage,
                                            personalDonePage: PersonalDonePage,
                                            businessDonePage: BusinessDonePage)
    extends FrontendController(mcc) with AuthorisedFunctions {

  implicit val config: AppConfig = appConfig

  val getDetails: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(Ok(petDetails(PetDetailsRequest.form)))
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  val continueUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/moreDetails"
  val changeUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/changeDetails"
  val doneUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/done"

  val postDetails: Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) {
      authProviderId =>
        val form = PetDetailsRequest.form.bindFromRequest()

        if (form.hasErrors)
          Future.successful(BadRequest(petDetails(form)))
        else {
          val requestData = form.value.get
          val initRequestPrepopulatedAccountInformation = InitRequestPrepopulatedData.from(
            accountType = requestData.accountType,
            name = requestData.accountName,
            sortCode = requestData.sortCode,
            accountNumber = requestData.accountNumber,
            rollNumber = requestData.rollNumber)

          connector.init(continueUrl, messages = requestMessages, prepopulatedData = initRequestPrepopulatedAccountInformation).map {
            case Some(initResponse) => SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}")
            case None               => InternalServerError
          }
        }
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  def getMoreDetails(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      Future.successful(Ok(morePetDetails(journeyId, MorePetDetailsRequest.form)))
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  def postMoreDetails(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) {
      authProviderId =>
        val form: Form[MorePetDetailsRequest] = MorePetDetailsRequest.form.bindFromRequest()

        if (form.hasErrors)
          Future.successful(BadRequest(morePetDetails(journeyId, form)))
        else {
          connector.complete(journeyId).map{
            case Some(r) if r.accountType == "personal" =>
              import PersonalCompleteResponse._
              addToSession(r)
            case Some(r) if r.accountType == "business" =>
              import BusinessCompleteResponse._
              addToSession(r)
          }.map { s =>
            SeeOther(routes.MakingPetsDigitalController.getCheckYourAnswers(journeyId).url)
                .withSession(form.value.flatMap(_.moreDetails).fold(s)(md => s + ("bavfefeMoreInformation" -> md)))
          }
        }
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  def getCheckYourAnswers(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {

      val (responseType, personalResponse, businessResponse, moreInformation) = retrieveFromSession()
      val result = responseType match {
        case "personal" =>
          import PersonalCompleteResponse._
          Ok(checkYourPersonalAnswersPage(personalResponse.get, changeUrl, s"$doneUrl/$journeyId"))
        case "business" =>
          import BusinessCompleteResponse._
          Ok(checkYourBusinessAnswersPage(businessResponse.get, changeUrl, s"$doneUrl/$journeyId"))
        case _                                   => InternalServerError
      }

      Future.successful(result)
    }
  }

  def changeYourAnswers(accountType: Option[String], accountName: Option[String], sortCode: Option[String], accountNumber: Option[String], rollNumber: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) { authProviderId =>

      connector.init(continueUrl = continueUrl,
        messages = requestMessages,
        prepopulatedData = Some(InitRequestPrepopulatedData(accountType, accountName, sortCode, accountNumber, rollNumber))).map {
        case Some(initResponse) => SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}")
        case None               => InternalServerError
      }
    }
  }

  def done(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) { authProviderId =>

      val (responseType, personalResponse, businessResponse, moreInformation) = retrieveFromSession()
      val result = responseType match {
        case "personal" => personalResponse.fold(InternalServerError: Result)(r => Ok(personalDonePage(r)))
        case "business" => businessResponse.fold(InternalServerError: Result)(r => Ok(businessDonePage(r)))
        case _          => InternalServerError
      }

      Future.successful(result)
    }
  }

  private def addToSession(completeResponse: CompleteResponse)(implicit request: Request[AnyContent]): Session = {
    val bavfeResponseType = completeResponse.accountType
    val bavfeResponse = {
      if (bavfeResponseType == "personal") Json.toJson(completeResponse.personal.get).toString()
      else if (bavfeResponseType == "business") Json.toJson(completeResponse.business.get).toString()
      else throw new IllegalStateException("Session is in inconsistent state")
    }

    request.session + ("bavfeResponseType" -> bavfeResponseType) + ("bavfeResponse" -> bavfeResponse)
  }

  private def retrieveFromSession()(implicit request: Request[AnyContent]): (String, Option[PersonalCompleteResponse], Option[BusinessCompleteResponse], Option[String]) = {
    val responseType = request.session.get("bavfeResponseType").get
    val responseJsonText = request.session.get("bavfeResponse").get
    val moreInformation = request.session.get("bavfefeMoreInformation")

    val (personal, business) = responseType match {
      case "personal" =>
        import PersonalCompleteResponse._
        (Json.fromJson[PersonalCompleteResponse](Json.parse(responseJsonText)).asOpt, None)
      case "business" =>
        import BusinessCompleteResponse._
        (None, Json.fromJson[BusinessCompleteResponse](Json.parse(responseJsonText)).asOpt)
    }

    (responseType, personal, business, moreInformation)
  }

  private def requestMessages(implicit messagesApi: MessagesApi) = {
    val english = messagesApi.preferred(Seq(Lang("en")))
    val welsh = messagesApi.preferred(Seq(Lang("cy")))
    Some(
      InitRequestMessages(
        en = Json.obj(
          "service.name" -> english("service.name"),
          "footer.accessibility.url" -> s"${appConfig.exampleExternalUrl}${english("footer.accessibility.url")}",
          "phaseBanner.tag" -> "BETA"
        ),
        cy = Some(
          Json.obj(
            "service.name" -> welsh("service.name"),
            "footer.accessibility.url" -> s"${appConfig.exampleExternalUrl}${welsh("footer.accessibility.url")}",
            "phaseBanner.tag" -> "BETA"
          )
        )
      )
    )
  }
}