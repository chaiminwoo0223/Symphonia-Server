package com.symphonia.common.exception.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.symphonia.IntegrationTest;
import com.symphonia.auth.helper.AuthHelper;
import com.symphonia.auth.presentation.AuthEndpoints;
import com.symphonia.common.exception.error.CommonErrorCode;
import com.symphonia.member.domain.entity.Member;
import com.symphonia.member.fixture.MemberFixture;
import com.symphonia.member.helper.MemberHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ValidationExceptionHandlerTest extends IntegrationTest {

    @Autowired private MemberHelper memberHelper;
    @Autowired private AuthHelper authHelper;

    @Nested
    @DisplayName("프레임워크 예외 응답")
    class HandleFrameworkException {

        @Nested
        @DisplayName("Content-Type이 지원되지 않는 경우")
        class WhenMediaTypeUnsupported {

            @Test
            @DisplayName("본문 status와 코드가 415에 맞는다")
            void shouldReturnUnsupportedMediaType() throws Exception {
                // when & then
                mockMvc.perform(
                                post(AuthEndpoints.LOGIN)
                                        .contentType(MediaType.TEXT_PLAIN)
                                        .content("text"))
                        .andExpect(status().isUnsupportedMediaType())
                        .andExpect(jsonPath("$.ok").value(false))
                        .andExpect(jsonPath("$.status").value(415))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode()));
            }
        }

        @Nested
        @DisplayName("지원하지 않는 HTTP 메서드인 경우")
        class WhenMethodNotAllowed {

            @Test
            @DisplayName("본문 status와 코드가 405에 맞는다")
            void shouldReturnMethodNotAllowed() throws Exception {
                // when & then
                mockMvc.perform(get(AuthEndpoints.LOGIN))
                        .andExpect(status().isMethodNotAllowed())
                        .andExpect(jsonPath("$.status").value(405))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.METHOD_NOT_ALLOWED.getCode()));
            }
        }

        @Nested
        @DisplayName("JSON 형식이 잘못된 경우")
        class WhenJsonMalformed {

            @Test
            @DisplayName("본문 status와 코드가 400에 맞는다")
            void shouldReturnBadRequest() throws Exception {
                // when & then
                mockMvc.perform(
                                post(AuthEndpoints.LOGIN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(CommonErrorCode.INVALID_JSON_FORMAT.getCode()));
            }
        }

        @Nested
        @DisplayName("경로 변수 타입이 맞지 않는 경우")
        class WhenPathVariableTypeMismatch {

            @Test
            @DisplayName("본문 status와 코드가 400에 맞는다")
            void shouldReturnBadRequest() throws Exception {
                // given
                Member member = memberHelper.save(MemberFixture.KAKAO);
                String token = authHelper.bearerTokenFor(member);

                // when & then
                mockMvc.perform(
                                delete("/api/v1/gatherings/{gatheringId}", "abc")
                                        .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(
                                jsonPath("$.data.code")
                                        .value(
                                                CommonErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH
                                                        .getCode()));
            }
        }
    }
}
