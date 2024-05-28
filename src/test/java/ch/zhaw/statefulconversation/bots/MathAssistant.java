package ch.zhaw.statefulconversation.bots;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.gson.Gson;

import ch.zhaw.statefulconversation.model.Agent;
import ch.zhaw.statefulconversation.model.OuterState;
import ch.zhaw.statefulconversation.model.State;
import ch.zhaw.statefulconversation.model.Storage;
import ch.zhaw.statefulconversation.model.Transition;
import ch.zhaw.statefulconversation.model.commons.actions.StaticExtractionAction;
import ch.zhaw.statefulconversation.model.commons.decisions.StaticDecision;
import ch.zhaw.statefulconversation.model.commons.states.EN_DynamicCauseAssessmentState;
import ch.zhaw.statefulconversation.repositories.AgentRepository;

@SpringBootTest
class MathAssistant {

    private static final String PROMPT_MATH_ASSISTANT = """
                    As a math assistant, your role is to help solve algebra problems and explain mathematical concepts.
                    Always respond with clear and concise answers.
                    """;
    private static final String PROMPT_ASK_QUESTION = """
                    Please ask your algebra question.
                    """;
    private static final String PROMPT_GENERATE_ANSWER = """
                    Here is the solution to your algebra problem: 
                    """;
    private static final String PROMPT_FOLLOW_UP = """
                    Do you have any other math questions?
                    """;

    @Autowired
    private AgentRepository repository;

    @Test
    void setUp() {
        // Define storage keys
        String storageKeyUserQuestion = "UserQuestion";
        String storageKeyBotAnswer = "BotAnswer";

        Gson gson = new Gson();

        // Initialize storage
        Storage storage = new Storage();
        storage.put(storageKeyUserQuestion, gson.toJsonTree(List.of("What is your algebra question?")));

        // Define states
        State followUpState = new State(PROMPT_FOLLOW_UP, "FollowUpState", PROMPT_FOLLOW_UP, List.of());
        State provideAnswer = new State(PROMPT_GENERATE_ANSWER, "ProvideAnswer", PROMPT_GENERATE_ANSWER, List.of(
                new Transition(List.of(new StaticDecision(PROMPT_FOLLOW_UP)),
                        List.of(new StaticExtractionAction(PROMPT_FOLLOW_UP, storage, storageKeyBotAnswer)),
                        followUpState)));
        State askQuestion = new EN_DynamicCauseAssessmentState(PROMPT_ASK_QUESTION, provideAnswer,
                        storage, storageKeyUserQuestion, storageKeyBotAnswer);

        // Define transitions
        Transition initialTransition = new Transition(
                List.of(new StaticDecision(PROMPT_ASK_QUESTION)),
                List.of(new StaticExtractionAction(PROMPT_GENERATE_ANSWER, storage, storageKeyBotAnswer)),
                askQuestion);

        // Create initial state
        State initialState = new OuterState(PROMPT_MATH_ASSISTANT, "GreetingState",
                        List.of(initialTransition), askQuestion);

        // Create and start the agent
        Agent agent = new Agent("MathAssistant", "Algebra problem-solving assistant", initialState, storage);
        agent.start();
        this.repository.save(agent);
    }
}
