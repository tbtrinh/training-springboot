package com.taibtrinh.cashcard;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CashCardApplicationTests
{
	@Autowired
	TestRestTemplate restTemplate;

	@Test
	void shouldReturnAllCashCardsWhenListIsRequested()
	{
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		int cashCardCount = documentContext.read("$.length()");
		assertThat(cashCardCount).isEqualTo(3);

		JSONArray ids = documentContext.read("$..id");
		assertThat(ids).containsExactlyInAnyOrder(99, 100, 101);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactlyInAnyOrder(123.45, 1.0, 150.00);
	}

	@Test
	void shouldReturnAPageOfCashCards()
	{
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards?page=0&size=1&sort=amount,desc", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(1);

		double amount = documentContext.read("$[0].amount");
		assertThat(amount).isEqualTo(150.00);
	}

	@Test
	void shouldReturnASortedPageOfCashCardsWithNoParametersAndUseDefaultValues()
	{
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		JSONArray page = documentContext.read("$[*]");
		assertThat(page.size()).isEqualTo(3);

		JSONArray amounts = documentContext.read("$..amount");
		assertThat(amounts).containsExactly(1.00, 123.45, 150.00);
	}

	@Test
	@DirtiesContext
	void shouldReturnACashCardWhenDataIsSaved()
	{
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99",String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(response.getBody());
		Number id = documentContext.read("$.id");
		assertThat(id).isEqualTo(99);

		Double amount = documentContext.read("$.amount");
		assertThat(amount).isEqualTo(123.45);
	}

	@Test
	void shouldNotReturnACashCardWithAnUnknownId()
	{
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/1000", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isBlank();
	}

	@Test
	void shouldNotReturnACashCardWhenUsingBadCredentials()
	{
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("BAD-USER", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		response = restTemplate
				.withBasicAuth("sarah1", "BAD-PASSWORD")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldRejectUsersWhoAreNotCardOwners() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("hank-owns-no-cards", "qrs456")
				.getForEntity("/cashcards/99", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldNotAllowAccessToCashCardsTheyDoNotOwn() {
		ResponseEntity<String> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/102", String.class); // kumar2's data
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldCreateANewCashCard() throws URISyntaxException
	{
		HttpHeaders headers = createCsrfHeaders();
		CashCard newCashCard = new CashCard(null, 250.00, null);
		RequestEntity<CashCard> request = new RequestEntity<>(newCashCard, headers, HttpMethod.POST, new URI("/cashcards"));
		ResponseEntity<Void> createResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.postForEntity("/cashcards", request, Void.class);

		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		URI locationOfNewCashCard = createResponse.getHeaders().getLocation();
		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity(locationOfNewCashCard, String.class);

		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");

		assertThat(id).isNotNull();
		assertThat(amount).isEqualTo(250.00);
	}

	@Test
	@DirtiesContext
	void shouldUpdateAnExistingCashCard() throws URISyntaxException
	{
		HttpHeaders headers = createCsrfHeaders();
		CashCard cashCardUpdate = new CashCard(null, 19.99, null);
		RequestEntity<CashCard> request = new RequestEntity<>(cashCardUpdate, headers, HttpMethod.PUT, new URI("/cashcards/99"));
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
		Number id = documentContext.read("$.id");
		Double amount = documentContext.read("$.amount");
		assertThat(id).isEqualTo(99);
		assertThat(amount).isEqualTo(19.99);
	}

	@Test
	void shouldNotUpdateACashCardThatDoesNotExist() throws URISyntaxException
	{
		HttpHeaders headers = createCsrfHeaders();
		CashCard unknownCard = new CashCard(null, 19.99, null);
		RequestEntity<CashCard> request = new RequestEntity<>(unknownCard, headers, HttpMethod.PUT, new URI("/cashcards/99999"));
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DirtiesContext
	void shouldDeleteAnExistingCashCard() throws URISyntaxException
	{
		HttpHeaders headers = createCsrfHeaders();
		RequestEntity<CashCard> request = new RequestEntity<>(null, headers, HttpMethod.DELETE, new URI("/cashcards/99"));
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99", HttpMethod.DELETE, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> getResponse = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.getForEntity("/cashcards/99", String.class);
		assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void shouldNotDeleteACashCardThatDoesNotExist() throws URISyntaxException
	{
		HttpHeaders headers = createCsrfHeaders();
		RequestEntity<CashCard> request = new RequestEntity<>(null, headers, HttpMethod.DELETE, new URI("/cashcards/99999"));
		ResponseEntity<Void> response = restTemplate
				.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards/99999", HttpMethod.DELETE, request, Void.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	/**
	 * This method creates the CSRF headers like a Javascript client would, by reading the cookie and then sending the
	 * value back in both the cookie and the header (the Double-Submit Cookie pattern).
	 */
	@SuppressWarnings("unchecked")
	private HttpHeaders createCsrfHeaders() {
		ResponseEntity<Void> response = restTemplate.withBasicAuth("sarah1", "abc123")
				.exchange("/cashcards", HttpMethod.HEAD, null, Void.class);
		HttpCookie cookie = HttpCookie.parse(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
				.stream()
				.filter(c -> c.getName().equals("XSRF-TOKEN"))
				.findFirst()
				.get();

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.COOKIE, cookie.toString());
		headers.add("X-XSRF-TOKEN", cookie.getValue());
		return headers;
	}
}
