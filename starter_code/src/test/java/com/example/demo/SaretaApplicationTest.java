package com.example.demo;

import com.example.demo.controllers.CartController;
import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = SareetaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestExecutionListeners({WithSecurityContextTestExecutionListener.class})
public class SaretaApplicationTest extends AbstractTestNGSpringContextTests {
    @LocalServerPort
    private int port;

    private static String BASE_URL;
    private static String LOGIN_URL;
    private static String USER_URL;
    private static String CART_URL;

    private static final String PASSWORD = "12345678";

    @Autowired
    UserController userController;

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    CartController cartController;

    TestRestTemplate restTemplate;

    @BeforeClass
    public void beforeClass() {
        restTemplate = new TestRestTemplate();
        BASE_URL = "http://localhost:" + port + "/";
        LOGIN_URL = BASE_URL + "login";
        USER_URL = BASE_URL + "api/user/";
        CART_URL = BASE_URL + "api/cart";
    }

    @Test
    public void canCreateUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("canCreateUserTest");
        req.setPassword(PASSWORD);
        req.setConfirmPassword(PASSWORD);
        ResponseEntity<User> res = userController.createUser(req);
        System.out.println("password:" + res.getBody().getPassword());
        Assert.assertTrue(res.getBody().getId() > 0);
        Assert.assertTrue(res.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void canDetectWrongConfirmPasswordWhenCreatingUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("canCreateUserTest");
        req.setPassword(PASSWORD);
        req.setConfirmPassword(PASSWORD + "9");
        ResponseEntity<User> res = userController.createUser(req);
        Assert.assertTrue(res.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    //    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    @Test
    public void canLogin() {
        createUser("canLoginTest");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map body = new HashMap();
        body.put("username", "canLoginTest");
        body.put("password", PASSWORD);

        HttpEntity entity = new HttpEntity(body, headers);

        System.out.println(LOGIN_URL);
        ResponseEntity<Object> res = restTemplate.postForEntity(LOGIN_URL, entity, Object.class);
        Assert.assertEquals(res.getStatusCode(), HttpStatus.OK);
        Assert.assertTrue(res.getHeaders().containsKey("Authorization"));
    }

    //    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    @Test
    public void canLoginViaHelper() throws InterruptedException {
        String token = createAndLogin("canLoginViaHelperTest");
        Assert.assertNotNull(token);
        Assert.assertTrue(token.contains("Bearer"));
    }

    @Test
    public void canBlockInvalidLogin() {
        HttpEntity entity = new RequestBuilder()
                .addBodyProperty("username", "invalid_user")
                .addBodyProperty("password", "invalid_pass")
                .build();

        ResponseEntity res = restTemplate.postForEntity(LOGIN_URL, entity, Object.class);
        Assert.assertTrue(res.getStatusCode().is4xxClientError());
        Assert.assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);
        Assert.assertFalse(res.getHeaders().containsKey("Authorization"));
    }

    @Test(dataProvider = "pagesNeededAuth")
    public void canGetPagesAfterLogin(String url, String testUsername) {
        String token = createAndLogin(testUsername);

        // can block when not logged in
        HttpEntity entity = new RequestBuilder()
                .build();
        ResponseEntity res = restTemplate.exchange(url, HttpMethod.GET, entity, User.class);
        System.out.println(res.getStatusCode());
        Assert.assertEquals(res.getStatusCode(), HttpStatus.UNAUTHORIZED);


        // should allow if token present
        HttpEntity authEntity = new RequestBuilder()
                .setAuthToken(token)
                .build();
        ResponseEntity authRes = restTemplate.exchange(url, HttpMethod.GET, authEntity, User.class);
        System.out.println(authRes.getStatusCode());
        Assert.assertTrue(authRes.getStatusCode().is2xxSuccessful() || authRes.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @DataProvider(name = "pagesNeededAuth")
    public Object[][] pagesNeededAuth() {
        return new Object[][]{
                {USER_URL + "canGetPagesAfterLogin", "canGetPagesAfterLogin"},
                {USER_URL + "canGetPageByUsername", "canGetPageByUsername"},
                {USER_URL + "id/2", "canGetUserDetailById2"},
                {USER_URL + "id/3", "canGetUserDetailById3"},
                {USER_URL + "id/4", "canGetUserDetailById4"},
                {USER_URL + "id/5", "canGetUserDetailById5"},
                {USER_URL + "id/6", "canGetUserDetailById6"},
                {USER_URL + "id/7", "canGetUserDetailById7"},
                {USER_URL + "id/8", "canGetUserDetailById8"},
                {USER_URL + "id/9", "canGetUserDetailById9"},
                {USER_URL + "id/10", "canGetUserDetailById10"},
        };
    }

    @Test
    public void canCreateUserViaRest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("username", "rest_user");
        body.put("password", PASSWORD);
        body.put("confirmPassword", PASSWORD);
        String url = USER_URL + "create";
        System.out.println(url);

        HttpEntity entity = new HttpEntity(body, headers);

        ResponseEntity<Object> res = restTemplate.postForEntity(url, entity, Object.class);
        Assert.assertEquals(res.getStatusCode(), HttpStatus.OK);
        Assert.assertTrue(res.getStatusCode().is2xxSuccessful());
    }

    private User createUser(String username) {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername(username);
        req.setPassword(PASSWORD);
        req.setConfirmPassword(PASSWORD);
        ResponseEntity<User> res = userController.createUser(req);
        return (User) res.getBody();
    }

    private String createAndLogin(String username) {
        String result = "";
        User u = createUser(username);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map body = new HashMap();
        body.put("username", username);
        body.put("password", PASSWORD);

        HttpEntity entity = new HttpEntity(body, headers);

        System.out.println(LOGIN_URL);
        try {
            ResponseEntity<Object> res = restTemplate.postForEntity(LOGIN_URL, entity, Object.class);
            if (res.getHeaders().containsKey("Authorization")) {
                result = res.getHeaders().get("Authorization").get(0);
            }
        } catch (Exception e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
        return result;
    }


    /*  Cart Controller */
    @Test
    public void canAddToCart() {
        // before auth
        String addToCartUrl = CART_URL + "/addToCart";

        ResponseEntity<Object> res = restTemplate.getForEntity(addToCartUrl, Object.class);
        System.out.println(res.getStatusCode());
        Assert.assertTrue(res.getStatusCode() == HttpStatus.UNAUTHORIZED);

        // after auth
        String auth = createAndLogin("canAddToCart");
        HttpEntity entity = new RequestBuilder()
                .setAuthToken(auth)
                .build();
        ResponseEntity res1 = restTemplate.postForEntity(addToCartUrl, entity, Object.class);
        System.out.println(res1.getStatusCode());
        Assert.assertEquals(res1.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void canRemoveFromCart() {
        String url = CART_URL + "/removeFromCart";

        // before auth
        ResponseEntity<Object> res = restTemplate.getForEntity(url, Object.class);
        System.out.println(res.getStatusCode());
        Assert.assertTrue(res.getStatusCode() == HttpStatus.UNAUTHORIZED);

        // after auth
        String auth = createAndLogin("canRemoveFromCart");
        HttpEntity entity = new RequestBuilder()
                .setAuthToken(auth)
                .build();
        ResponseEntity res1 = restTemplate.postForEntity(url, entity, Object.class);
        System.out.println(res1.getStatusCode());
        Assert.assertEquals(res1.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void canModifyCartViaController() {
        String username = "canAddToCartViaController";

        User u = createUser(username);

        Item i = new Item();
        i.setName("testItem");
        i.setPrice(BigDecimal.valueOf(100));
        i.setDescription("testItem");
        i = itemRepository.save(i);

        ModifyCartRequest req = new ModifyCartRequest();
        req.setUsername(username);
        req.setItemId(i.getId());
        req.setQuantity(20);
        ResponseEntity<Cart> res = cartController.addTocart(req);

        Assert.assertTrue(res.getStatusCode().is2xxSuccessful());
        Cart c = res.getBody();

        // remove from cart
        req.setQuantity(10);
        ResponseEntity<Cart> rmRes = cartController.removeFromcart(req);
        Assert.assertTrue(rmRes.getStatusCode().is2xxSuccessful());
    }

    /*  OrderController */
    @Test
    public void canSubmitOrder() {

        String ORDER_URL = BASE_URL + "api/order";
        String username = "canAccessOrder";
        String submitUrl = ORDER_URL + "/submit/" + username;
        String getHistoryUrl = ORDER_URL + "/history/" + username;

        HttpEntity entity = new RequestBuilder()
                .build();

        ResponseEntity fail1 = restTemplate.getForEntity(getHistoryUrl, Object.class);

        // should block without auth
        Assert.assertEquals(fail1.getStatusCode(), HttpStatus.UNAUTHORIZED);

        String auth = createAndLogin(username);
        HttpEntity authedEntity = new RequestBuilder()
                .setAuthToken(auth)
                .build();
        ResponseEntity success1 = restTemplate.exchange(getHistoryUrl, HttpMethod.GET, authedEntity, Object.class);
        ResponseEntity success2 = restTemplate.exchange(submitUrl, HttpMethod.POST, authedEntity, Object.class);

        // should show 200 or 404 instead of 403 forbidden due to authed
        Assert.assertTrue(success1.getStatusCode() == HttpStatus.OK || success1.getStatusCode() == HttpStatus.NOT_FOUND);
        Assert.assertTrue(success2.getStatusCode() == HttpStatus.OK || success2.getStatusCode() == HttpStatus.NOT_FOUND);
    }
}
