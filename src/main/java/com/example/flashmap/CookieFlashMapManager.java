package com.example.flashmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.AbstractFlashMapManager;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.web.servlet.DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME;

@Component(FLASH_MAP_MANAGER_BEAN_NAME)
public class CookieFlashMapManager extends AbstractFlashMapManager {
  private static final String COOKIE_NAME = "flashMap";

  private final ObjectMapper objectMapper;

  public CookieFlashMapManager(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
    return Optional.ofNullable(cookie)
        .map(this::retrieveFlashMapFromCookieValue)
        .orElse(new ArrayList<>());
  }

  private List<FlashMap> retrieveFlashMapFromCookieValue(Cookie cookie) {
    try {
      byte[] decode = Base64.getDecoder().decode(cookie.getValue());
      return this.objectMapper.readValue(new String(decode, UTF_8), new TypeReference<List<FlashMap>>() {
      });
    } catch (IOException e) {
      throw new RuntimeException("Error decoding flash map", e);
    }
  }

  @Override
  protected void updateFlashMaps(List<FlashMap> flashMaps, HttpServletRequest request, HttpServletResponse response) {
    String encodedValue = this.encodeFlashMaps(flashMaps);
    Cookie cookie = new Cookie(COOKIE_NAME, encodedValue);
    cookie.setPath("/");
    response.addCookie(cookie);
  }

  private String encodeFlashMaps(List<FlashMap> flashMaps) {
    try {
      String json = this.objectMapper.writeValueAsString(flashMaps);
      return Base64.getEncoder().encodeToString(json.getBytes(UTF_8));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error encoding flash map", e);
    }
  }

  @Override
  protected Object getFlashMapsMutex(HttpServletRequest request) {
    return request;
  }
}
