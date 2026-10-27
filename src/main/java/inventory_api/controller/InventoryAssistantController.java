package inventory_api.controller;

import inventory_api.dto.AssistantRequest;
import inventory_api.dto.AssistantResponse;
import inventory_api.service.InventoryAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class InventoryAssistantController {
    private final InventoryAssistantService assistant;
    public InventoryAssistantController(InventoryAssistantService assistant) { this.assistant = assistant; }

    @PostMapping("/inventory")
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) { return assistant.answer(request.question()); }
}
