/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.bankaccountverificationexamplefrontend.config.AppConfig
import uk.gov.hmrc.bankaccountverificationexamplefrontend.example.html.PetDetails
import uk.gov.hmrc.bankaccountverificationexamplefrontend.views.html._
import uk.gov.hmrc.bankaccountverificationexamplefrontend._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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
                                            businessDonePage: BusinessDonePage)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) with AuthorisedFunctions {

  implicit val config: AppConfig = appConfig

  val getDetails: Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      val (petDetailsRequest, _, _, _, _) = retrieveFromSession()
      val form = petDetailsRequest.fold(PetDetailsRequest.form)(PetDetailsRequest.form.fill)
      Future.successful(Ok(petDetails(form)))
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  val continueUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/moreDetails"
  val changeAccountTypeUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/changeDetails"
  val doneUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/postCheckYourDetails"
  val changePetDetailsUrl = s"${appConfig.exampleExternalUrl}/bank-account-verification-example-frontend/start"

  val postDetails: Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) {
      authProviderId =>
        val petDetailsRequestForm = PetDetailsRequest.form.bindFromRequest()

        if (petDetailsRequestForm.hasErrors)
          Future.successful(BadRequest(petDetails(petDetailsRequestForm)))
        else {
          val petDetailsRequest = petDetailsRequestForm.get
          val (_, accountType, personal, business, _) = retrieveFromSession()

          val initRequestPrepopulatedAccountInformation = accountType.fold(None: Option[InitRequestPrepopulatedData]) { at =>
              val x@(att, an, sc, anum, rn) = if(at == "personal")
                (at, personal.get.accountName, personal.get.sortCode, personal.get.accountNumber, personal.get.rollNumber)
              else
                (at, business.get.companyName, business.get.sortCode, business.get.accountNumber, business.get.rollNumber)

            InitRequestPrepopulatedData.from(accountType = att, name = an, sortCode = sc, accountNumber = anum, rollNumber = rn)
          }

          val newSession = addToSession(petDetailsRequest)
          connector.init(continueUrl, messages = requestMessages, prepopulatedData = initRequestPrepopulatedAccountInformation).map {
            case Some(initResponse) => SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}").withSession(newSession)
            case None               => InternalServerError
          }
        }
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  def getMoreDetails(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      val moreInformation = request.session.get("bavfefeMoreInformation").map(Json.parse).flatMap{ j =>
          import MorePetDetailsRequest.formats._
        Json.fromJson(j).asOpt
      }
      Future.successful(Ok(morePetDetails(journeyId, moreInformation.fold(MorePetDetailsRequest.form){mi => MorePetDetailsRequest.form.fill(mi)})))
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
          val moreDetails: MorePetDetailsRequest = form.get

          connector.complete(journeyId).map{
            case Some(r) if r.accountType == "personal" =>
              addToSession(r, moreDetails)
            case Some(r) if r.accountType == "business" =>
              addToSession(r, moreDetails)
          }.map { s =>
              import MorePetDetailsRequest.formats._
              val moreDetailsJsonText = Json.toJson[MorePetDetailsRequest](form.get).toString()
            SeeOther(routes.MakingPetsDigitalController.getCheckYourAnswers(journeyId).url)
                .withSession(s)
          }
        }
    } recoverWith { case _ =>
      Future.successful(SeeOther(appConfig.authLoginStubUrl))
    }
  }

  def getCheckYourAnswers(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised() {
      val (petDetailsRequest, responseType, personalResponse, businessResponse, moreInformation) = retrieveFromSession()
      val result = responseType match {
        case Some("personal") =>
          Ok(checkYourPersonalAnswersPage(journeyId, petDetailsRequest.get, personalResponse.get, moreInformation.get, changePetDetailsUrl, changeAccountTypeUrl, doneUrl))
        case Some("business") =>
          Ok(checkYourBusinessAnswersPage(journeyId, petDetailsRequest.get, businessResponse.get, moreInformation.get, changePetDetailsUrl, changeAccountTypeUrl, doneUrl))
        case _                                   => InternalServerError
      }
      Future.successful(result)
    }
  }

  def changeYourAnswers(accountType: Option[String], accountName: Option[String], companyName: Option[String], sortCode: Option[String], accountNumber: Option[String], rollNumber: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) { authProviderId =>

        val effectiveAccountName = accountType.flatMap{
          case "personal" => accountName
          case "business" => companyName
        }

      connector.init(continueUrl = continueUrl,
        messages = requestMessages,
        prepopulatedData = Some(InitRequestPrepopulatedData(accountType, effectiveAccountName, sortCode, accountNumber, rollNumber))).map {
        case Some(initResponse) => SeeOther(s"${appConfig.bavfWebBaseUrl}${initResponse.startUrl}")
        case None               => InternalServerError
      }
    }
  }

  def postCheckYourAnswers(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    authorised().retrieve(AuthProviderId.retrieval) { authProviderId =>

      val ((petDetailsRequest, responseType, personalResponse, businessResponse, moreInformation), newSession) = retrieveFromAndClearSession()
      val result = responseType match {
        case Some("personal") => personalResponse.fold(InternalServerError: Result)(r => Ok(personalDonePage(petDetailsRequest.get, r, moreInformation.get)))
        case Some("business") => businessResponse.fold(InternalServerError: Result)(r => Ok(businessDonePage(petDetailsRequest.get, r, moreInformation.get)))
        case _                => InternalServerError
      }

      Future.successful(result.withSession(newSession))
    }
  }

  private def addToSession(petDetailsRequest: PetDetailsRequest)(implicit request: Request[AnyContent]): Session = {
    import PetDetailsRequest.formats._
    (request.session + ("mpdDetails" -> Json.toJson(petDetailsRequest).toString))
  }

  private def addToSession(completeResponse: CompleteResponse, moreDetails: MorePetDetailsRequest)(implicit request: Request[AnyContent]): Session = {
    val bavfeResponseType = completeResponse.accountType
    val bavfeResponse = {
      if (bavfeResponseType == "personal") Json.toJson(completeResponse.personal.get).toString()
      else if (bavfeResponseType == "business") Json.toJson(completeResponse.business.get).toString()
      else throw new IllegalStateException("Session is in inconsistent state")
    }

    val moreDetailsText = {
      import MorePetDetailsRequest.formats._
      Json.toJson(moreDetails).toString()
    }

    request.session +
        ("bavfeResponseType" -> bavfeResponseType) +
        ("bavfeResponse" -> bavfeResponse) +
        ("bavfefeMoreInformation" -> moreDetailsText)
  }

  private def retrieveFromAndClearSession()(implicit request: Request[AnyContent]): ((Option[PetDetailsRequest], Option[String], Option[PersonalCompleteResponse], Option[BusinessCompleteResponse], Option[MorePetDetailsRequest]), Session) = {
    val result = retrieveFromSession()
    (result, (request.session -- Seq("mpdDetails", "bavfeResponseType", "bavfeResponse", "bavfefeMoreInformation")))
  }

  private def retrieveFromSession()(implicit request: Request[AnyContent]): (Option[PetDetailsRequest], Option[String], Option[PersonalCompleteResponse], Option[BusinessCompleteResponse], Option[MorePetDetailsRequest]) = {
    val petDetailsRequestText = request.session.get("mpdDetails")
    val responseType = request.session.get("bavfeResponseType")
    val responseJsonText = request.session.get("bavfeResponse")
    val moreInformationText = request.session.get("bavfefeMoreInformation")

    val petDetailsRequest = petDetailsRequestText.flatMap{ pdr =>
      import PetDetailsRequest.formats._
      Json.fromJson[PetDetailsRequest](Json.parse(pdr)).asOpt
    }

    val (personal, business) = (responseType, responseJsonText) match {
      case (Some("personal"), Some(rjt)) =>
        import PersonalCompleteResponse._
        (Json.fromJson[PersonalCompleteResponse](Json.parse(rjt)).asOpt, None)
      case (Some("business"), Some(rjt)) =>
        import BusinessCompleteResponse._
        (None, Json.fromJson[BusinessCompleteResponse](Json.parse(rjt)).asOpt)
      case (None, None) => (None, None)
    }
    val moreInformation = {
      import MorePetDetailsRequest.formats._
      moreInformationText.fold(None: Option[MorePetDetailsRequest]){mit => Json.fromJson[MorePetDetailsRequest](Json.parse(mit)).asOpt}
    }

    (petDetailsRequest, responseType, personal, business, moreInformation)
  }

  private def requestMessages(implicit messagesApi: MessagesApi) = {
    val english = messagesApi.preferred(Seq(Lang("en")))
    val welsh = messagesApi.preferred(Seq(Lang("cy")))
    Some(
      InitRequestMessages(
        en = Json.obj(
          "service.name" -> english("service.name"),
          "label.accountDetails.heading.business" -> "Business bank or building society account details",
          "label.accountDetails.heading.personal" -> "Personal bank or building society account details",
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