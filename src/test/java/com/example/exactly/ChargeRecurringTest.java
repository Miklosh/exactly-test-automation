package com.example.exactly;

import com.example.exactly.model.*;
import com.example.exactly.util.PaymentStatusUI;
import com.example.exactly.util.ProcessingResultCode;
import com.example.exactly.util.TransactionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class ChargeRecurringTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    private static String API_URL;
    private static String CREATE_TRANSACTION_URI;
    private static String AUTH_KEY;
    private static String PROJECT_ID;
    private static String VALID_CARD;
    private static String INSUFFICIENT_FUNDS_CARD;
    private static String AUTH_FAILED_CARD;
    private static String UNSETTLED_TRANSACTION_CARD;
    private static String RECURRING_TRANSACTION_URL;
    private static String TRANSACTION_STATUS_URL;
    private final static String AUTH_HEADER_NAME = "Authorization";
    private final static String CHARGE_TYPE = "charge-recurring";
    private static final String CARD_PAYMENT_METHOD = "card";
    public static final String CURRENCY = "EUR";
    public static final String AMOUNT = "10.99";
    public static final String INITIATOR = "customer";
    public static final String UNSCHEDULED = "unscheduled";
    public static final String CARD_HOLDER_NAME = "MYKOLA KOZHEMIAKA";

    @BeforeAll
    static void init() {
        try (InputStream input = ChargeRecurringTest.class.getClassLoader().getResourceAsStream("application-test.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                log.error("Sorry, unable to find properties");
                return;
            }
            prop.load(input);
            API_URL = prop.getProperty("api.url");
            CREATE_TRANSACTION_URI = prop.getProperty("transaction.uri");
            AUTH_KEY = prop.getProperty("api.key");
            PROJECT_ID = prop.getProperty("project.id");
            VALID_CARD = prop.getProperty("valid.card");
            INSUFFICIENT_FUNDS_CARD = prop.getProperty("insufficient.funds.card");
            AUTH_FAILED_CARD = prop.getProperty("auth.fail.card");
            UNSETTLED_TRANSACTION_CARD = prop.getProperty("unsettled.transaction.card");

            RECURRING_TRANSACTION_URL = API_URL + CREATE_TRANSACTION_URI;
            TRANSACTION_STATUS_URL = API_URL + CREATE_TRANSACTION_URI + "/{transactionId}";
        } catch (IOException ex) {
            log.error("Error during properties fetch", ex);
        }
    }

    @Test
    public void happyPass() throws JsonProcessingException {
        String referenceId = getReferenceId();
        log.info("referenceId: {}", referenceId);
        ChargeRecurringInitialRequest initialRequest = getChargeRecurringInitialRequest(referenceId);

        String initialRequestJson = mapper.writeValueAsString(initialRequest);
        log.info("REQUEST: " + initialRequestJson);

        String responseJson = sendPostRequest(RECURRING_TRANSACTION_URL, initialRequestJson);
        log.info("RESPONSE string: {}", responseJson);
        ChargeRecurringInitialResponse response = mapper.readValue(responseJson, ChargeRecurringInitialResponse.class);
        log.info("RESPONSE object: {}", response);

        String refPaymentUrl = response.getIncluded().get(0).getAttributes().getUrl();
        log.info("refPaymentUrl: {}", refPaymentUrl);

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(refPaymentUrl);
        fillCardDataAndPay(driver, VALID_CARD);
        String paymentStatus = getUIPaymentStatus(driver);
        log.info("Payment status: {}", paymentStatus);
        assertEquals(PaymentStatusUI.SUCCESS, paymentStatus);

        ChargeRecurringSubseqRequest subseqRequest = getChargeRecurringSubseqRequest(referenceId);

        String subseqRequestJson = mapper.writeValueAsString(subseqRequest);
        log.info("SUBSEQ REQUEST: " + initialRequestJson);

        String subSeqResponseJson = sendPostRequest(RECURRING_TRANSACTION_URL, subseqRequestJson);
        log.info("RESPONSE string: {}", subSeqResponseJson);
        ChargeRecurringSubseqResponse subSeqResponse = mapper.readValue(subSeqResponseJson, ChargeRecurringSubseqResponse.class);
        log.info("RESPONSE object: {}", subSeqResponse);

        assertNotNull(subSeqResponse.getData());
        assertNotNull(subSeqResponse.getData().getId());

        String transactionId = subSeqResponse.getData().getId();

        String transactionStatusResponseJson = sendGetRequest(TRANSACTION_STATUS_URL, transactionId);
        log.info("transactionStatusResponseJson {}", transactionStatusResponseJson);
        assertNotNull(transactionStatusResponseJson);
        TransactionStatusResponse transactionStatusResponse = mapper.readValue(transactionStatusResponseJson, TransactionStatusResponse.class);
        log.info("transactionStatusResponse: {}", transactionStatusResponse);
        assertNotNull(transactionStatusResponse);
        assertNotNull(transactionStatusResponse.getData());
        assertEquals(CHARGE_TYPE, transactionStatusResponse.getData().getType());
        assertNotNull(transactionStatusResponse.getData().getAttributes());
        assertNotNull(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(PROJECT_ID, transactionStatusResponse.getData().getAttributes().getProjectId());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getRecurring());
        assertEquals(INITIATOR, transactionStatusResponse.getData().getAttributes().getRecurring().getInitiator());
        assertEquals(UNSCHEDULED, transactionStatusResponse.getData().getAttributes().getRecurring().getScenario());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getStatus());
        assertEquals(TransactionStatus.PROCESSING, transactionStatusResponse.getData().getAttributes().getStatus());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertEquals(AMOUNT, transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertEquals(CURRENCY, transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        assertEquals(CARD_PAYMENT_METHOD, transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        driver.close();
    }

    @Test
    public void authenticationFailedTest() throws JsonProcessingException {
        String referenceId = getReferenceId();
        log.info("referenceId: {}", referenceId);
        ChargeRecurringInitialRequest initialRequest = getChargeRecurringInitialRequest(referenceId);
        String initialRequestJson = mapper.writeValueAsString(initialRequest);
        log.info("REQUEST: " + initialRequestJson);

        String responseJson = sendPostRequest(RECURRING_TRANSACTION_URL, initialRequestJson);
        log.info("RESPONSE string: {}", responseJson);
        ChargeRecurringInitialResponse response = mapper.readValue(responseJson, ChargeRecurringInitialResponse.class);
        log.info("RESPONSE object: {}", response);

        String refPaymentUrl = response.getIncluded().get(0).getAttributes().getUrl();
        log.info("refPaymentUrl: {}", refPaymentUrl);

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(refPaymentUrl);
        fillCardDataAndPay(driver, AUTH_FAILED_CARD);
        String paymentStatus = getUIPaymentStatus(driver);
        log.info("Payment status: {}", paymentStatus);
        assertEquals(PaymentStatusUI.FAILED, paymentStatus);

        String transactionId = response.getData().getId();
        log.info("transactionId: {}", transactionId);
        String transactionStatusResponseJson = sendGetRequest(TRANSACTION_STATUS_URL, transactionId);
        log.info("transactionStatusResponseJson {}", transactionStatusResponseJson);
        assertNotNull(transactionStatusResponseJson);
        TransactionStatusResponse transactionStatusResponse = mapper.readValue(transactionStatusResponseJson, TransactionStatusResponse.class);
        log.info("transactionStatusResponse: {}", transactionStatusResponse);
        assertNotNull(transactionStatusResponse);
        assertNotNull(transactionStatusResponse.getData());
        assertEquals(CHARGE_TYPE, transactionStatusResponse.getData().getType());
        assertNotNull(transactionStatusResponse.getData().getAttributes());
        assertNotNull(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(PROJECT_ID, transactionStatusResponse.getData().getAttributes().getProjectId());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getRecurring());
        assertEquals(UNSCHEDULED, transactionStatusResponse.getData().getAttributes().getRecurring().getScenario());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getStatus());
        assertEquals(TransactionStatus.FAILED, transactionStatusResponse.getData().getAttributes().getStatus());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertEquals(AMOUNT, transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertEquals(CURRENCY, transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getResultCode());
        assertEquals(ProcessingResultCode.FAILED_AUTHENTICATION, transactionStatusResponse.getData().getAttributes().getProcessing().getResultCode());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        assertEquals(CARD_PAYMENT_METHOD, transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        driver.close();
    }

    @Test
    public void insufficientFundsTest() throws JsonProcessingException {
        String referenceId = getReferenceId();
        log.info("referenceId: {}", referenceId);
        ChargeRecurringInitialRequest initialRequest = getChargeRecurringInitialRequest(referenceId);
        String initialRequestJson = mapper.writeValueAsString(initialRequest);
        log.info("REQUEST: " + initialRequestJson);

        String responseJson = sendPostRequest(RECURRING_TRANSACTION_URL, initialRequestJson);
        log.info("RESPONSE string: {}", responseJson);
        ChargeRecurringInitialResponse response = mapper.readValue(responseJson, ChargeRecurringInitialResponse.class);
        log.info("RESPONSE object: {}", response);

        String refPaymentUrl = response.getIncluded().get(0).getAttributes().getUrl();
        log.info("refPaymentUrl: {}", refPaymentUrl);

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(refPaymentUrl);
        fillCardDataAndPay(driver, INSUFFICIENT_FUNDS_CARD);
        String paymentStatus = getUIPaymentStatus(driver);
        log.info("Payment status: {}", paymentStatus);
        assertEquals(PaymentStatusUI.FAILED, paymentStatus);

        String transactionId = response.getData().getId();

        String transactionStatusResponseJson = sendGetRequest(TRANSACTION_STATUS_URL, transactionId);
        log.info("transactionStatusResponseJson {}", transactionStatusResponseJson);
        assertNotNull(transactionStatusResponseJson);
        TransactionStatusResponse transactionStatusResponse = mapper.readValue(transactionStatusResponseJson, TransactionStatusResponse.class);
        log.info("transactionStatusResponse: {}", transactionStatusResponse);
        assertNotNull(transactionStatusResponse);
        assertNotNull(transactionStatusResponse.getData());
        assertEquals(CHARGE_TYPE, transactionStatusResponse.getData().getType());
        assertNotNull(transactionStatusResponse.getData().getAttributes());
        assertNotNull(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(PROJECT_ID, transactionStatusResponse.getData().getAttributes().getProjectId());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getRecurring());
        assertEquals(UNSCHEDULED, transactionStatusResponse.getData().getAttributes().getRecurring().getScenario());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getStatus());
        assertEquals(TransactionStatus.FAILED, transactionStatusResponse.getData().getAttributes().getStatus());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertEquals(AMOUNT, transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertEquals(CURRENCY, transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getResultCode());
        assertEquals(ProcessingResultCode.FAILED_INSUFFICIENT_FUNDS, transactionStatusResponse.getData().getAttributes().getProcessing().getResultCode());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        assertEquals(CARD_PAYMENT_METHOD, transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        driver.close();
    }

    @Test
    public void unsettledTransactionTest() throws JsonProcessingException {
        String referenceId = getReferenceId();
        log.info("referenceId: {}", referenceId);
        ChargeRecurringInitialRequest initialRequest = getChargeRecurringInitialRequest(referenceId);
        String initialRequestJson = mapper.writeValueAsString(initialRequest);
        log.info("REQUEST: " + initialRequestJson);

        String responseJson = sendPostRequest(RECURRING_TRANSACTION_URL, initialRequestJson);
        log.info("RESPONSE string: {}", responseJson);
        ChargeRecurringInitialResponse response = mapper.readValue(responseJson, ChargeRecurringInitialResponse.class);
        log.info("RESPONSE object: {}", response);

        String refPaymentUrl = response.getIncluded().get(0).getAttributes().getUrl();
        log.info("refPaymentUrl: {}", refPaymentUrl);

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(refPaymentUrl);
        fillCardDataAndPay(driver, UNSETTLED_TRANSACTION_CARD);
        String paymentStatus = getUIPaymentStatus(driver);
        log.info("Payment status: {}", paymentStatus);
        assertEquals(PaymentStatusUI.SUCCESS, paymentStatus);

        String transactionId = response.getData().getId();

        String transactionStatusResponseJson = sendGetRequest(TRANSACTION_STATUS_URL, transactionId);
        log.info("transactionStatusResponseJson {}", transactionStatusResponseJson);
        assertNotNull(transactionStatusResponseJson);
        TransactionStatusResponse transactionStatusResponse = mapper.readValue(transactionStatusResponseJson, TransactionStatusResponse.class);
        log.info("transactionStatusResponse: {}", transactionStatusResponse);
        assertNotNull(transactionStatusResponse);
        assertNotNull(transactionStatusResponse.getData());
        assertEquals(CHARGE_TYPE, transactionStatusResponse.getData().getType());
        assertNotNull(transactionStatusResponse.getData().getAttributes());
        assertNotNull(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(transactionId, transactionStatusResponse.getData().getId());
        assertEquals(PROJECT_ID, transactionStatusResponse.getData().getAttributes().getProjectId());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getRecurring());
        assertEquals(UNSCHEDULED, transactionStatusResponse.getData().getAttributes().getRecurring().getScenario());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getStatus());
        assertEquals(TransactionStatus.SETTLED, transactionStatusResponse.getData().getAttributes().getStatus());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertEquals(AMOUNT, transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertEquals(CURRENCY, transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getResultCode());
        assertEquals(ProcessingResultCode.SUCCESS, transactionStatusResponse.getData().getAttributes().getProcessing().getResultCode());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        assertEquals(CARD_PAYMENT_METHOD, transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        driver.close();
    }

    private static String getUIPaymentStatus(WebDriver driver) {
        return new WebDriverWait(driver, Duration.of(20, ChronoUnit.SECONDS))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class=\"payment-status__title\"]"))).getText();
    }

    private static void fillCardDataAndPay(WebDriver driver, String paymentCard) {
        new WebDriverWait(driver, Duration.of(20, ChronoUnit.SECONDS))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@class=\"input input--cardholder\"]")))
                .sendKeys(CARD_HOLDER_NAME);
        driver.findElement(By.xpath("//input[@class=\"input input--card-number\"]"))
                .sendKeys(paymentCard);
        driver.findElement(By.xpath("//input[@class=\"input input--exp-month\"]"))
                .sendKeys("12/26");
        driver.findElement(By.xpath("//input[@class=\"input input--csc\"]"))
                .sendKeys("123");

        driver.findElement(By.xpath("//button[@class=\"btn btn--pay btn--block\"]"))
                .click();
    }

    private static ChargeRecurringSubseqRequest getChargeRecurringSubseqRequest(String referenceId) {
        ChargeRecurringSubseqRequest subseqRequest = new ChargeRecurringSubseqRequest();
        ChargeRecurringSubseqRequest.Data subseqData = new ChargeRecurringSubseqRequest.Data();
        ChargeRecurringSubseqRequest.Attributes subseqAttr = new ChargeRecurringSubseqRequest.Attributes();
        ChargeRecurringSubseqRequest.Recurring subseqRec = new ChargeRecurringSubseqRequest.Recurring();
        subseqRec.setInitiator(INITIATOR);
        subseqAttr.setRecurring(subseqRec);
        subseqAttr.setProjectId(PROJECT_ID);
        subseqAttr.setPaymentMethod(CARD_PAYMENT_METHOD);
        subseqAttr.setAmount(AMOUNT);
        subseqAttr.setCurrency(CURRENCY);
        subseqAttr.setOriginalReferenceId(referenceId);
        subseqData.setAttributes(subseqAttr);
        subseqData.setType(CHARGE_TYPE);
        subseqRequest.setData(subseqData);
        return subseqRequest;
    }

    private static String sendPostRequest(String url, String requestJson) {
        Header authHeader = new Header(AUTH_HEADER_NAME, AUTH_KEY);
        RequestSpecification spec = given().header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY);
        return given().header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY)
                .body(requestJson)
                .when()
                .post(url)
                .then().log().all()
                .extract().response().body().asString();
    }

    private static String sendGetRequest(String url, String... requestParams) {
        Header authHeader = new Header(AUTH_HEADER_NAME, AUTH_KEY);
        assertNotNull(requestParams);
        String reqParam1 = requestParams[0];
        assertNotNull(reqParam1);
        log.info(" requestParams[0]: {}", reqParam1);
        RequestSpecification spec = given().header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY);
        return given()
                .header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY)
                .pathParam("transactionId", reqParam1)
                .when()
                .get(url)
                .then().log().all()
                .extract().response().body().asString();
    }

    private static ChargeRecurringInitialRequest getChargeRecurringInitialRequest(String referenceId) {
        ChargeRecurringInitialRequest initialRequest = new ChargeRecurringInitialRequest();
        ChargeRecurringInitialRequest.Data data = new ChargeRecurringInitialRequest.Data();
        ChargeRecurringInitialRequest.Attributes attributes = new ChargeRecurringInitialRequest.Attributes();
        ChargeRecurringInitialRequest.Recurring recurring = new ChargeRecurringInitialRequest.Recurring();
        recurring.setScenario(UNSCHEDULED);
        recurring.setExpectedInitiators(List.of(INITIATOR));
        attributes.setProjectId(PROJECT_ID);
        attributes.setPaymentMethod(CARD_PAYMENT_METHOD);
        attributes.setAmount(AMOUNT);
        attributes.setCurrency(CURRENCY);
        attributes.setRecurring(recurring);
        attributes.setReferenceId(referenceId);
        data.setType(CHARGE_TYPE);
        data.setAttributes(attributes);
        initialRequest.setData(data);
        return initialRequest;
    }

    private static String getReferenceId() {
        int length = 10;
        boolean useLetters = true;
        boolean useNumbers = false;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

}
