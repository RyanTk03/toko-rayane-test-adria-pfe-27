package tech.tokorayane.ebankbot.controllers;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import tech.tokorayane.ebankbot.agents.EBankAIAgent;

@RestController
public class EbankChatbotController {
    private final EBankAIAgent eBankAIAgent;

    public EbankChatbotController(EBankAIAgent eBankAIAgent) {
        this.eBankAIAgent = eBankAIAgent;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(name = "query") String query) {
        return this.eBankAIAgent.chat("0", new Prompt(query));
    }

    @GetMapping("/chatStream")
    public Flux<String> chatStream(@RequestParam(name = "query") String query) {
        return this.eBankAIAgent.chatStream("0", new Prompt(query));
    }
}
