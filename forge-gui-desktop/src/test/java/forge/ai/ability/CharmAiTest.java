package forge.ai.ability;

import forge.ai.*;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class CharmAiTest extends AITest {

    //hack for TokenAI which has 80% probability of allowing to cast token
    public void tweakAiProfile(Player ai){
        LobbyPlayerAi lai = (LobbyPlayerAi) ai.getLobbyPlayer();
        lai.setAiProfile("Default");
        try {
            Field field = AiProfileUtil.class.getDeclaredField("loadedProfiles");
            field.setAccessible(true);
            Map<String, Map<AiProps, String>> profiles = (Map<String, Map<AiProps, String>>)field.get(null);
            var map = profiles.get(lai.getAiProfile());
            map.put(AiProps.TOKEN_GENERATION_ABILITY_CHANCE, "100");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@code  Rootcast Apprenticeship} can choose 3 modes (with canRepeatModes), but only one has valid target.
     */
    @Test
    public void testCharmAiCanRepeatTheSameMode() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        tweakAiProfile(ai);

        addCards("Forest", 4, ai);
        addCardToZone("Rootcast Apprenticeship", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AiController aiCtrl = new AiController(ai, game);
        List<SpellAbility> list = aiCtrl.chooseSpellAbilityToPlay();
        assertNotNull(list);
        SpellAbility sa = list.get(0);
        for (AbilitySub sub : sa.getChosenList()) {
            assertEquals(sub.getDescription(), "Target player creates a 1/1 green Squirrel creature token.");
        }
    }

    /**
     * {@code  Rootcast Apprenticeship} can choose 3 modes (with canRepeatModes), but only two has valid target.
     */
    @Test
    public void testCharmAiCanRepeatModes() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        tweakAiProfile(ai);

        addCards("Forest", 4, ai);
        addCardToZone("Rootcast Apprenticeship", ai, ZoneType.Hand);
        addCardToZone("Metalwork Colossus", opponent, ZoneType.Battlefield);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AiController aiCtrl = new AiController(ai, game);
        List<SpellAbility> list = aiCtrl.chooseSpellAbilityToPlay();
        assertNotNull(list);
        SpellAbility sa = list.get(0);
        String chosenListAsString = sa.getChosenList().stream().map(AbilitySub::getDescription).collect(Collectors.joining("; "));
        assertEquals(StringUtils.countMatches(chosenListAsString, "Target player creates a 1/1 green Squirrel creature token."), 2);
        assertEquals(StringUtils.countMatches(chosenListAsString, "Target opponent sacrifices a nontoken artifact."), 1);
    }

    /**
     * {@code Verdant Confluence} can not be cast - all modes have invalid targets (missing creature and empty library and graveyard)
     */
    @Test
    public void testCharmAiDoesNotCastCharmWithCanRepeatModesWithoutValidTargets() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Forest", 4, ai);
        addCardToZone("Verdant Confluence", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AiController aiCtrl = new AiController(ai, game);
        List<SpellAbility> list = aiCtrl.chooseSpellAbilityToPlay();
        assertNull(list);
    }

    /**
     * {@code Cabaretti Confluence} has three modes, but only one (exile target artifact) with valid target ({@code Portal to Phyrexia}).
     * AI should choose other modes or choose the same multiple times even if only one mode is resolved.
     */
    @Test
    public void testCharmAiWithOnlyOneValidMode() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        tweakAiProfile(ai);

        addCards("Forest", 2, ai);
        addCards("Mountain", 2, ai);
        addCards("Plains", 2, ai);
        addCardToZone("Cabaretti Confluence", ai, ZoneType.Hand);
        addCardToZone("Portal to Phyrexia", opponent, ZoneType.Battlefield);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AiController aiCtrl = new AiController(ai, game);
        List<SpellAbility> list = aiCtrl.chooseSpellAbilityToPlay();
        assertNotNull(list);
        SpellAbility sa = list.get(0);
        String chosenListAsString = sa.getChosenList().stream().map(AbilitySub::getDescription).collect(Collectors.joining("; "));
        assertTrue(chosenListAsString.contains("Exile target artifact or enchantment."));
    }

}
