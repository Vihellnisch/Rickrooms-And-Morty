package com.rickandmorty.pet;

import com.rickandmorty.network.PetSpeechPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What amplified pets say: a bubble over their head for everyone nearby (creeper warnings also go to
 * their owner's chat). Each kind of pet has its own way of putting things (the lines are translations, counted in
 * {@link PetLines}); they don't talk over themselves, and don't repeat a topic too often.
 */
public final class PetSpeech {
    public enum Topic {
        FITTED(0), IDLE(1800), ATTACK(300), KILL(120), OWNER_HURT(300), LOW_HEALTH(600), WARNING(400), CREEPER(160),
        HEAL_OWNER(0), FETCH(200), MODULE(0), REMOVED(0), DEATH(0), NIGHT(6000);

        /** Ticks before it'll bring this up again. */
        final int gap;

        Topic(int gap) {
            this.gap = gap;
        }

        String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final int QUIET = 80;
    private static final Map<Mob, Long> LAST_SAID = new WeakHashMap<>();
    private static final Map<Mob, Map<Topic, Long>> LAST_TOPIC = new WeakHashMap<>();

    private PetSpeech() {
    }

    /** Which set of lines it uses. */
    public static String species(Mob mob) {
        if (mob instanceof Wolf) return "dog";
        if (mob instanceof Cat || mob instanceof Ocelot) return "cat";
        if (mob instanceof Parrot) return "bird";
        if (mob instanceof Fox) return "fox";
        if (mob instanceof Llama) return "llama";
        if (mob instanceof AbstractHorse) return "horse";
        return "pet";
    }

    /** The name an unnamed pet picks for itself when it gets clever (dogs, of course, want to be Snowball). */
    static String chooseName(Mob mob) {
        String[] names = switch (species(mob)) {
            case "dog" -> new String[]{"Snowball"};
            case "cat" -> new String[]{"Professor Whiskers", "Duchess", "Sir Pounce", "Nebula"};
            case "bird" -> new String[]{"Captain Squawk", "Doctor Feathers", "Beakthoven"};
            case "fox" -> new String[]{"Reynard", "Professor Sly", "Ember"};
            case "llama" -> new String[]{"Lord Spitsworth", "Dolly", "Count Fluffington"};
            case "horse" -> new String[]{"Thunderhoof", "Sir Gallops", "Neighpoleon"};
            default -> new String[]{"Genius", "Einstein", "Brainy"};
        };
        return names[mob.getRandom().nextInt(names.length)];
    }

    /** Says something about this, unless it spoke too recently (important lines always get said). */
    public static boolean say(Mob mob, Topic topic, boolean important) {
        if (!(mob.level() instanceof ServerLevel level) || !PetBrain.isAmplified(mob)) return false;
        long now = level.getGameTime();
        Map<Topic, Long> topics = LAST_TOPIC.computeIfAbsent(mob, m -> new EnumMap<>(Topic.class));
        if (!important) {
            Long said = LAST_SAID.get(mob), aboutThis = topics.get(topic);
            if (said != null && now - said < QUIET) return false;
            if (aboutThis != null && now - aboutThis < topic.gap) return false;
        }
        String group = PetLines.COUNTS.containsKey(species(mob) + "." + topic.key()) ? species(mob) : "pet";
        Integer count = PetLines.COUNTS.get(group + "." + topic.key());
        if (count == null) return false;
        Player owner = PetBrain.owner(mob);
        Component name = owner != null ? owner.getName() : Component.literal("friend");
        Component line = Component.translatable("pet.rickrooms." + group + "." + topic.key() + "." + mob.getRandom().nextInt(count), name);
        LAST_SAID.put(mob, now);
        topics.put(topic, now);
        PetBrain.sendNear(mob, new PetSpeechPayload(mob.getId(), line), 48.0);
        // only creeper warnings go to chat too (everything else would flood it), so you see them whichever way you face
        if (topic == Topic.CREEPER && owner instanceof ServerPlayer player && player.level() == level && player.distanceToSqr(mob) < 64 * 64) {
            player.sendSystemMessage(Component.translatable("chat.rickrooms.pet_says",
                    mob.getDisplayName().copy().withStyle(ChatFormatting.AQUA), line));
        }
        mob.playAmbientSound();
        return true;
    }
}
