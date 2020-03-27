package no.difi.meldingsutveksling.receipt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.clock.FixedClockConfig;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationQueryInput;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.difi.meldingsutveksling.nextmove.ConversationDirection.OUTGOING;
import static no.difi.meldingsutveksling.receipt.service.ConversationTestData.dpiConversation;
import static no.difi.meldingsutveksling.receipt.service.ConversationTestData.dpoConversation;
import static no.difi.meldingsutveksling.receipt.service.RestDocumentationCommon.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@Import({FixedClockConfig.class, JacksonConfig.class, JacksonMockitoConfig.class})
@WebMvcTest(ConversationController.class)
@AutoConfigureMoveRestDocs
@TestPropertySource("classpath:/config/application-test.properties")
@ActiveProfiles("test")
@ComponentScan(basePackageClasses = WebhookFilterParser.class)
public class ConversationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ConversationRepository conversationRepository;

    @Test
    public void find() throws Exception {
        given(conversationRepository.findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Conversation> content = Arrays.asList(dpoConversation(), dpiConversation());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/conversations")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/find",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(
                                parameterWithName("messageId").optional().description("Filter on messageId"),
                                parameterWithName("conversationId").optional().description("Filter on conversationId"),
                                parameterWithName("receiverIdentifier").optional().description("Filter on receiverIdentifier"),
                                parameterWithName("senderIdentifier").optional().description("Filter on senderIdentifier"),
                                parameterWithName("messageReference").optional().description("Filter on messageReference"),
                                parameterWithName("messageTitle").optional().description("Filter on message title"),
                                parameterWithName("pollable").optional().description("Filter on pollable (true/false)"),
                                parameterWithName("finished").optional().description("Filter on finished (true/false)"),
                                parameterWithName("direction").optional().description(String.format("Filter on direction. Can be one of: %s", Arrays.stream(ConversationDirection.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))))
                        ).and(getPagingParameterDescriptors()),
                        responseFields()
                                .and(conversationDescriptors("content[]."))
                                .and(pageDescriptors())
                                .andWithPrefix("pageable.", pageableDescriptors())
                        )
                );

        verify(conversationRepository).findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSearch() throws Exception {

        given(conversationRepository.findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Conversation> content = Collections.singletonList(dpoConversation());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/conversations")
                        .param("serviceIdentifier", ServiceIdentifier.DPO.name())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/find/search"));

        verify(conversationRepository).findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findSorting() throws Exception {
        given(conversationRepository.findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Conversation> content = Collections.singletonList(dpoConversation());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/conversations")
                        .param("sort", "lastUpdated,asc")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/find/sorting"));

        verify(conversationRepository).findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class));
    }

    @Test
    public void findPaging() throws Exception {

        given(conversationRepository.findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Conversation> content = Collections.singletonList(dpiConversation());
                    return new PageImpl<>(content, invocation.getArgument(1), 31L);
                });

        mvc.perform(
                get("/api/conversations")
                        .param("page", "3")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/find/paging"));

        verify(conversationRepository).findWithMessageStatuses(any(ConversationQueryInput.class), any(Pageable.class));
    }

    @Test
    public void getById() throws Exception {
        Conversation conversation = dpoConversation();

        given(conversationRepository.findByIdAndDirection(anyLong(), any()))
                .willReturn(Optional.of(conversation));

        mvc.perform(
                get("/api/conversations/{id}", conversation.getId())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("id").description("The numeric id of the conversation to be retrieved.")
                        ),
                        responseFields()
                                .and(conversationDescriptors(""))
                        )
                );

        verify(conversationRepository).findByIdAndDirection(conversation.getId(), OUTGOING);
    }

    @Test
    public void getByMessageId() throws Exception {
        Conversation conversation = dpoConversation();

        given(conversationRepository.findByMessageIdAndDirection(anyString(), any()))
                .willReturn(Collections.singletonList(conversation));

        mvc.perform(
                get("/api/conversations/messageId/{messageId}", conversation.getMessageId())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/get-by-message-id",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        pathParameters(
                                parameterWithName("messageId").description("The messageId. Typically a UUID.")
                        ),
                        responseFields()
                                .and(conversationDescriptors(""))
                        )
                );

        verify(conversationRepository).findByMessageIdAndDirection(conversation.getMessageId(), OUTGOING);
    }

    @Test
    public void queuedConversations() throws Exception {
        given(conversationRepository.findByPollable(anyBoolean(), any(Pageable.class)))
                .willAnswer(invocation -> {
                    List<Conversation> content = Arrays.asList(dpoConversation(), dpiConversation());
                    return new PageImpl<>(content, invocation.getArgument(1), content.size());
                });

        mvc.perform(
                get("/api/conversations/queue")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andDo(document("conversations/queue",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestHeaders(
                                getDefaultHeaderDescriptors()
                        ),
                        requestParameters(getPagingParameterDescriptors()),
                        responseFields()
                                .and(conversationDescriptors("content[]."))
                                .and(pageDescriptors())
                                .andWithPrefix("pageable.", pageableDescriptors())
                        )
                );

        verify(conversationRepository).findByPollable(eq(true), any(Pageable.class));
    }
}
