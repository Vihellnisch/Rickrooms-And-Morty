package com.rickandmorty.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rickandmorty.RickAndMortyMod;
import com.rickandmorty.entity.PortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The blender portal's machinery: segmented mechanical tentacles that shoot out of the portal,
 * curl around the victim and hold on, then two arms with spinning saw blades that come out after
 * them and saw back and forth. Everything is drawn in world-aligned space around the portal.
 */
public final class BlenderArms {
    private static final ResourceLocation ARM = RickAndMortyMod.id("textures/entity/blender_arm.png");
    private static final ResourceLocation SAW = RickAndMortyMod.id("textures/entity/blender_saw.png");
    private static final int SEGMENTS = 14;
    /** Where the arms were last reaching, so they can pull back after the victim is gone. */
    private static final Map<Object, Vec3> LAST_TARGET = new WeakHashMap<>();
    private static final float[][] GRABBERS = {{-0.55f, 0.6f}, {0.55f, 0.6f}, {-0.6f, -0.45f}, {0.6f, -0.45f}};
    private static final float[][] GRIPS = {{-0.3f, 0.3f}, {0.3f, 0.3f}, {-0.25f, -0.35f}, {0.25f, -0.35f}};

    private BlenderArms() {
    }

    public static void render(PortalEntity portal, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        int phase = portal.getPhase();
        float age = portal.phaseAge(partialTick);
        float reach = switch (phase) {
            case PortalEntity.GRAB -> Mth.clamp(age / (PortalEntity.GRAB_TICKS * 0.7f), 0.0f, 1.0f);
            case PortalEntity.BLEND -> 1.0f;
            case PortalEntity.RETRACT -> 1.0f - Mth.clamp(age / PortalEntity.RETRACT_TICKS, 0.0f, 1.0f);
            default -> 0.0f;
        };
        float sawReach = phase == PortalEntity.BLEND ? Mth.clamp(age / 10.0f, 0.0f, 1.0f) : phase == PortalEntity.RETRACT ? reach : 0.0f;
        render(portal, reach, sawReach, false, portal.tickCount + partialTick, portal.getPosition(partialTick),
                portal.normal(), portal.right(), portal.up(), portal.getVictim(), partialTick, 1.0f, pose, buffers, light);
    }

    /**
     * The machinery itself, around a ring centred on {@code origin} (world position of the render
     * origin) facing {@code n}, with {@code r} and {@code u} across it. {@code reach}: how far the
     * grabbing tentacles are out (0..1); {@code sawReach}: the same for the saw arms. When
     * {@code thrash}, there's nothing to hold and everything flails about.
     */
    public static void render(Object key, float reach, float sawReach, boolean thrash, float time, Vec3 origin, Vec3 n, Vec3 r, Vec3 u,
                              @Nullable LivingEntity victim, float partialTick, float size, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 target;
        float width = 0.6f, height = 1.8f;
        if (victim != null) {
            target = victim.getPosition(partialTick).add(0, victim.getBbHeight() * 0.55, 0).subtract(origin);
            width = victim.getBbWidth();
            height = victim.getBbHeight();
            LAST_TARGET.put(key, target);
        } else {
            target = LAST_TARGET.getOrDefault(key, n.scale(1.5));
        }
        double side = target.dot(n) >= 0 ? 1.0 : -1.0;
        Vec3 out = n.scale(side);
        if (reach <= 0.0f && sawReach <= 0.0f) return;

        VertexConsumer arm = buffers.getBuffer(RenderType.entityCutoutNoCull(ARM));
        PoseStack.Pose p = pose.last();
        // grabbing tentacles
        for (int i = 0; i < GRABBERS.length && reach > 0.0f; i++) {
            Vec3 anchor = r.scale(GRABBERS[i][0] * size).add(u.scale(GRABBERS[i][1] * size)).add(out.scale(0.05));
            Vec3 grip = thrash ? flail(out, r, u, time, i)
                    : target.add(r.scale(GRIPS[i][0] * Math.max(0.5, width) * 1.4)).add(0, GRIPS[i][1] * height * 0.8, 0);
            Vec3 wobble = u.scale(Mth.sin(time * 0.35f + i * 1.7f) * 0.18).add(r.scale(Mth.cos(time * 0.3f + i) * 0.12));
            Vec3 c1 = anchor.add(out.scale(1.1 * size)).add(wobble);
            Vec3 c2 = grip.add(out.scale(0.5)).add(0, 0.35, 0).subtract(wobble);
            Vec3 tip = tentacle(p, arm, light, anchor, c1, c2, grip, reach, 0.13f * size, 0.075f * size);
            if (reach > 0.85f) claw(p, arm, light, tip, grip.subtract(c2).normalize(), time + i * 10);
        }
        // saw arms, once it's got hold
        if (sawReach > 0.0f) {
            Vec3[] blades = new Vec3[2];
            for (int i = 0; i < 2; i++) {
                float s = i == 0 ? -1.0f : 1.0f;
                Vec3 anchor = r.scale(0.3 * s * size).add(u.scale(0.15 * size)).add(out.scale(0.05));
                // sawing: back and forth through the middle of whatever it's holding
                Vec3 cut = thrash ? flail(out, r, u, time * 1.3f, i + 5)
                        : target.add(r.scale(0.12 * s)).add(0, Mth.sin(time * 0.9f + i * Mth.PI) * height * 0.28, 0)
                        .subtract(out.scale(0.15 + 0.1 * Mth.sin(time * 0.6f + i)));
                Vec3 c1 = anchor.add(out.scale(0.8 * size)).add(u.scale(0.5 * size));
                Vec3 c2 = cut.add(out.scale(0.7)).add(0, 0.25, 0);
                Vec3 tip = tentacle(p, arm, light, anchor, c1, c2, cut, sawReach, 0.15f * size, 0.1f * size);
                blades[i] = sawReach > 0.6f ? tip : null;
            }
            // the blades after all the arms: asking for another buffer ends the arms' one
            VertexConsumer saw = buffers.getBuffer(RenderType.entityCutoutNoCull(SAW));
            // each blade stands edge-on to the ring, so it slices into whatever is in front
            Vec3 across = Math.abs(out.y) < 0.9 ? new Vec3(0, 1, 0) : r;
            for (int i = 0; i < 2; i++) {
                if (blades[i] != null) blade(p, saw, light, blades[i], out, across, time * 55.0f * (i == 0 ? 1 : -1), 0.55f * size);
            }
        }
    }

    /** Where a flailing arm is reaching for, this instant. */
    private static Vec3 flail(Vec3 out, Vec3 r, Vec3 u, float time, int i) {
        return out.scale(1.6 + 0.7 * Mth.sin(time * 0.7f + i * 1.3f))
                .add(r.scale(1.6 * Mth.sin(time * 0.9f + i * 2.1f)))
                .add(u.scale(1.3 * Mth.cos(time * 1.1f + i * 0.8f)));
    }

    /** Draws a jointed tentacle along a cubic Bezier, up to {@code reach} of the way. Returns where its tip is. */
    private static Vec3 tentacle(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                 float reach, float thick, float thin) {
        Vec3 prev = p0;
        int count = Math.max(1, Math.round(SEGMENTS * reach));
        for (int i = 1; i <= count; i++) {
            float t = reach * i / (float) count;
            Vec3 next = bezier(p0, p1, p2, p3, t);
            float radius = Mth.lerp(t, thick, thin);
            beam(pose, consumer, light, prev, next, radius, (i % 2) * 0.5f);
            prev = next;
        }
        return prev;
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float t) {
        float s = 1 - t;
        return a.scale(s * s * s).add(b.scale(3 * s * s * t)).add(c.scale(3 * s * t * t)).add(d.scale(t * t * t));
    }

    /** Three curled prongs at the end of a grabbing arm, opening and closing slightly. */
    private static void claw(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 tip, Vec3 dir, float time) {
        Vec3 side = Math.abs(dir.y) < 0.9 ? dir.cross(new Vec3(0, 1, 0)).normalize() : dir.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = side.cross(dir).normalize();
        float open = 0.5f + Mth.sin(time * 0.4f) * 0.1f;
        for (int k = 0; k < 3; k++) {
            double angle = k * Math.PI * 2 / 3;
            Vec3 spread = side.scale(Math.cos(angle)).add(up.scale(Math.sin(angle)));
            Vec3 knuckle = tip.add(dir.scale(0.16)).add(spread.scale(0.2 * open));
            Vec3 point = knuckle.add(dir.scale(0.22)).subtract(spread.scale(0.14));
            beam(pose, consumer, light, tip, knuckle, 0.05f, 0.0f);
            beam(pose, consumer, light, knuckle, point, 0.035f, 0.5f);
        }
    }

    /** A spinning circular saw blade centred on {@code center}, in the plane of {@code a} and {@code b}. */
    private static void blade(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 center, Vec3 a, Vec3 b, float spinDegrees, float radius) {
        float rad = spinDegrees * Mth.DEG_TO_RAD;
        Vec3 x = a.scale(Mth.cos(rad)).add(b.scale(Mth.sin(rad))).scale(radius);
        Vec3 y = b.scale(Mth.cos(rad)).subtract(a.scale(Mth.sin(rad))).scale(radius);
        Vec3 normal = a.cross(b).normalize();
        Vec3 c0 = center.subtract(x).subtract(y), c1 = center.add(x).subtract(y), c2 = center.add(x).add(y), c3 = center.subtract(x).add(y);
        quad(pose, consumer, light, c0, c1, c2, c3, 0, 0, 1, 1, normal);
    }

    /** A square beam from a to b with the given half-width (an arm segment). */
    private static void beam(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 a, Vec3 b, float half, float v0) {
        Vec3 dir = b.subtract(a);
        double length = dir.length();
        if (length < 1.0E-4) return;
        dir = dir.scale(1.0 / length);
        Vec3 side = Math.abs(dir.y) < 0.95 ? dir.cross(new Vec3(0, 1, 0)).normalize() : dir.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = side.cross(dir).normalize();
        Vec3 s = side.scale(half), w = up.scale(half);
        float v1 = v0 + 0.5f;
        Vec3[] corners = {s.add(w), s.scale(-1).add(w), s.scale(-1).subtract(w), s.subtract(w)};
        Vec3[] normals = {up, side.scale(-1), up.scale(-1), side};
        for (int f = 0; f < 4; f++) {
            Vec3 k0 = corners[f], k1 = corners[(f + 1) % 4];
            quad(pose, consumer, light, a.add(k0), a.add(k1), b.add(k1), b.add(k0), 0.0f, v0, 0.25f, v1, normals[f]);
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float u0, float v0, float u1, float v1, Vec3 normal) {
        vertex(pose, consumer, light, a, u0, v0, normal);
        vertex(pose, consumer, light, b, u1, v0, normal);
        vertex(pose, consumer, light, c, u1, v1, normal);
        vertex(pose, consumer, light, d, u0, v1, normal);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, int light, Vec3 p, float u, float v, Vec3 normal) {
        consumer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
