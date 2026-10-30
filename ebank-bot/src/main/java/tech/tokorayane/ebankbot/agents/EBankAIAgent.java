package tech.tokorayane.ebankbot.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class EBankAIAgent {
    private final ChatClient chatClient;

    public EBankAIAgent(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, ToolCallbackProvider tools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Vous un assistant qui se charge de répondre aux question
                        de l'utilisateur à propos des clients et des comptes bancaires, en fonction du contexte fourni.
                        Si aucun contexte n'est fourni, répond avec JE NE SAIS PAS
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools)
                .build();
    }

    public String chat(String conversationId, Prompt prompt) {
        return this.chatClient.prompt(prompt)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .call().content();
    }

    public Flux<String> chatStream(String conversationId, Prompt prompt) {
        return this.chatClient.prompt(prompt)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId
                ))
                .stream().content();
    }
}
