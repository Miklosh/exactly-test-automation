package com.example.exactly;

import com.example.exactly.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
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
    private final static String AUTH_HEADER_NAME = "Authorization";
    private final static String CHARGE_TYPE = "charge-recurring";
    private static final String PAYMENT_METHOD = "card";
    public static final String CURRENCY = "EUR";
    public static final String AMOUNT = "10.99";
    public static final String INITIATOR = "customer";
    public static final String UNSCHEDULED = "unscheduled";
    public static final String TRANSACTION_STATUS = "processing";

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
        } catch (IOException ex) {
            log.error("Error during properties fetch", ex);
        }
    }

    @Test
    public void happyPass() throws JsonProcessingException {
        ChargeRecurringInitialRequest initialRequest = new ChargeRecurringInitialRequest();
        ChargeRecurringInitialRequest.Data data = new ChargeRecurringInitialRequest.Data();
        ChargeRecurringInitialRequest.Attributes attributes = new ChargeRecurringInitialRequest.Attributes();
        ChargeRecurringInitialRequest.Recurring recurring = new ChargeRecurringInitialRequest.Recurring();
        recurring.setScenario(UNSCHEDULED);
        recurring.setExpectedInitiators(List.of(INITIATOR));
        attributes.setProjectId(PROJECT_ID);
        attributes.setPaymentMethod(PAYMENT_METHOD);
        attributes.setAmount(AMOUNT);
        attributes.setCurrency(CURRENCY);
        attributes.setRecurring(recurring);
        String referenceId = getReferenceId();
        log.info("referenceId: {}", referenceId);
        attributes.setReferenceId(referenceId);
        data.setType(CHARGE_TYPE);
        data.setAttributes(attributes);
        initialRequest.setData(data);

        String initialRequestJson = mapper.writeValueAsString(initialRequest);
        log.info("REQUEST: " + initialRequestJson);

        Header authHeader = new Header(AUTH_HEADER_NAME, AUTH_KEY);
        String responseJson = given()
                .header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY)
                .body(initialRequestJson)
                .when()
                .post(API_URL + CREATE_TRANSACTION_URI)
                .then().log().all()
                .extract().response().body().asString();
        log.info("RESPONSE string: {}", responseJson);
        ChargeRecurringInitialResponse response = mapper.readValue(responseJson, ChargeRecurringInitialResponse.class);
        log.info("RESPONSE object: {}", response);

        String refPaymentUrl = response.getIncluded().get(0).getAttributes().getUrl();
        log.info("refPaymentUrl: {}", refPaymentUrl);


        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get(refPaymentUrl);
        new WebDriverWait(driver, Duration.of(20, ChronoUnit.SECONDS))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@class=\"input input--cardholder\"]")))
                .sendKeys("MYKOLA KOZHEMIAKA");
        driver.findElement(By.xpath("//input[@class=\"input input--card-number\"]"))
                .sendKeys(VALID_CARD);
        driver.findElement(By.xpath("//input[@class=\"input input--exp-month\"]"))
                .sendKeys("12/26");
        driver.findElement(By.xpath("//input[@class=\"input input--csc\"]"))
                .sendKeys("123");

        driver.findElement(By.xpath("//button[@class=\"btn btn--pay btn--block\"]"))
                .click();
        String paymentStatus = new WebDriverWait(driver, Duration.of(20, ChronoUnit.SECONDS))
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@class=\"payment-status__title\"]"))).getText();
        log.info("Payment status: {}", paymentStatus);
        assertEquals("Payment successful!", paymentStatus);


        ChargeRecurringSubseqRequest subseqRequest = new ChargeRecurringSubseqRequest();
        ChargeRecurringSubseqRequest.Data subseqData = new ChargeRecurringSubseqRequest.Data();
        ChargeRecurringSubseqRequest.Attributes subseqAttr = new ChargeRecurringSubseqRequest.Attributes();
        ChargeRecurringSubseqRequest.Recurring subseqRec = new ChargeRecurringSubseqRequest.Recurring();
        subseqRec.setInitiator(INITIATOR);
        subseqAttr.setRecurring(subseqRec);
        subseqAttr.setProjectId(PROJECT_ID);
        subseqAttr.setPaymentMethod(PAYMENT_METHOD);
        subseqAttr.setAmount(AMOUNT);
        subseqAttr.setCurrency(CURRENCY);
        subseqAttr.setOriginalReferenceId(referenceId);
        subseqData.setAttributes(subseqAttr);
        subseqData.setType(CHARGE_TYPE);
        subseqRequest.setData(subseqData);

        String subseqRequestJson = mapper.writeValueAsString(subseqRequest);
        log.info("SUBSEQ REQUEST: " + initialRequestJson);

        String subSeqResponseJson = given()
                .header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY)
                .body(subseqRequestJson)
                .when()
                .post(API_URL + CREATE_TRANSACTION_URI)
                .then().log().all()
                .extract().response().body().asString();
        log.info("RESPONSE string: {}", subSeqResponseJson);
        ChargeRecurringSubseqResponse subSeqResponse = mapper.readValue(subSeqResponseJson, ChargeRecurringSubseqResponse.class);
        log.info("RESPONSE object: {}", subSeqResponse);

        assertNotNull(subSeqResponse.getData());
        assertNotNull(subSeqResponse.getData().getId());

        String transactionId = subSeqResponse.getData().getId();

        String transactionStatusResponseJson = given()
                .header(authHeader)
                .contentType("application/vnd.api+json")
                .accept(ContentType.ANY)
                .pathParam("transactionId", transactionId)
                .when()
                .get(API_URL + CREATE_TRANSACTION_URI + "/{transactionId}")
                .then().log().all()
                .extract().response().body().asString();
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
        assertEquals(TRANSACTION_STATUS, transactionStatusResponse.getData().getAttributes().getStatus());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertEquals(AMOUNT, transactionStatusResponse.getData().getAttributes().getProcessing().getAmount());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertEquals(CURRENCY, transactionStatusResponse.getData().getAttributes().getProcessing().getCurrency());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod());
        assertNotNull(transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        assertEquals(PAYMENT_METHOD, transactionStatusResponse.getData().getAttributes().getPaymentMethod().getType());
        driver.close();
    }

    private static String getReferenceId() {
        int length = 10;
        boolean useLetters = true;
        boolean useNumbers = false;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }

}
