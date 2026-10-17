package com.rickandmorty.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Finds the head on any animal's model, so the helmet fits whatever it's put on: wolves, cats,
 * parrots, foxes, horses, llamas, camels, and other mods' animals made the usual way.
 * <p>
 * Which part is the head: on models built as one tree, the part called "...head" nearest the root;
 * on the older kind, the parts the model itself draws as its head (or a field called head). Of that
 * part's boxes, the head is the highest of the big ones (so not a llama's neck or a camel's snout).
 * Found once per model; each frame, the head's position is read off the model as it's posed.
 */
final class PetHeads {
    /** The part to start from, the path from it to the head's part ("" is itself), and the head box. */
    record Head(ModelPart start, String path, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, boolean ageable) {
    }

    private static final Map<EntityModel<?>, Optional<Head>> FOUND = new WeakHashMap<>();
    private static Method headParts;
    private static Field scaleHead, babyYHeadOffset, babyZHeadOffset, babyHeadScale;

    private PetHeads() {
    }

    static Optional<Head> find(EntityModel<?> model) {
        return FOUND.computeIfAbsent(model, PetHeads::search);
    }

    private static Optional<Head> search(EntityModel<?> model) {
        try {
            ModelPart start = null;
            boolean ageable = false;
            if (model instanceof HierarchicalModel<?> tree) {
                start = tree.root();
            } else if (model instanceof AgeableListModel<?> ageableModel) {
                if (headParts == null) {
                    headParts = AgeableListModel.class.getDeclaredMethod("headParts");
                    headParts.setAccessible(true);
                }
                for (Object part : (Iterable<?>) headParts.invoke(ageableModel)) {
                    start = (ModelPart) part;
                    break;
                }
                ageable = true;
            }
            if (start == null) start = headField(model);
            if (start == null) return Optional.empty();

            Map<String, List<ModelPart.Cube>> cubes = new LinkedHashMap<>();
            start.visit(new PoseStack(), (pose, path, index, cube) -> cubes.computeIfAbsent(path, p -> new ArrayList<>()).add(cube));
            if (cubes.isEmpty()) return Optional.empty();
            String best = null;
            for (String path : cubes.keySet()) {
                String last = path.substring(path.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
                if (last.endsWith("head") && (best == null || depth(path) < depth(best))) best = path;
            }
            if (best == null) {
                for (String path : cubes.keySet()) {
                    if (best == null || depth(path) < depth(best)) best = path;
                }
            }
            // a parrot's "head" is the lower half; the top of its skull is "head2"
            if (cubes.containsKey(best + "/head2")) best = best + "/head2";
            ModelPart.Cube head = headBox(cubes.get(best));
            return Optional.of(new Head(start, best, head.minX, head.minY, head.minZ, head.maxX, head.maxY, head.maxZ, ageable));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private static int depth(String path) {
        return path.isEmpty() ? 0 : path.split("/").length;
    }

    /** The highest of the part's big boxes. */
    private static ModelPart.Cube headBox(List<ModelPart.Cube> cubes) {
        float biggest = 0;
        for (ModelPart.Cube cube : cubes) biggest = Math.max(biggest, volume(cube));
        ModelPart.Cube best = cubes.get(0);
        for (ModelPart.Cube cube : cubes) {
            if (volume(cube) >= biggest * 0.25f && (volume(best) < biggest * 0.25f || cube.minY < best.minY)) best = cube;
        }
        return best;
    }

    private static float volume(ModelPart.Cube cube) {
        return (cube.maxX - cube.minX) * (cube.maxY - cube.minY) * (cube.maxZ - cube.minZ);
    }

    private static ModelPart headField(EntityModel<?> model) throws IllegalAccessException {
        ModelPart named = null;
        for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != ModelPart.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                if (name.equals("head") || (named == null && name.contains("head"))) {
                    field.setAccessible(true);
                    ModelPart part = (ModelPart) field.get(model);
                    if (name.equals("head")) return part;
                    named = part;
                }
            }
        }
        return named;
    }

    /**
     * Moves the pose onto the head's part as the model is posed right now (the box's corners are then
     * in that part's own pixels). False if the head couldn't be followed.
     */
    static boolean poseOnHead(Head head, EntityModel<?> model, PoseStack pose) {
        PoseStack walk = new PoseStack();
        walk.last().pose().set(pose.last().pose());
        walk.last().normal().set(pose.last().normal());
        if (head.ageable() && model.young) babyHead((AgeableListModel<?>) model, walk);
        Matrix4f[] at = {null};
        Matrix3f[] normal = {null};
        head.start().visit(walk, (p, path, index, cube) -> {
            if (at[0] == null && path.equals(head.path())) {
                at[0] = new Matrix4f(p.pose());
                normal[0] = new Matrix3f(p.normal());
            }
        });
        if (at[0] == null) return false;
        pose.last().pose().set(at[0]);
        pose.last().normal().set(normal[0]);
        return true;
    }

    /** The same shrinking and shifting the model gives a baby's head. */
    private static void babyHead(AgeableListModel<?> model, PoseStack pose) {
        try {
            if (scaleHead == null) {
                scaleHead = field("scaleHead");
                babyYHeadOffset = field("babyYHeadOffset");
                babyZHeadOffset = field("babyZHeadOffset");
                babyHeadScale = field("babyHeadScale");
            }
            if (scaleHead.getBoolean(model)) {
                float scale = 1.5f / babyHeadScale.getFloat(model);
                pose.scale(scale, scale, scale);
            }
            pose.translate(0.0f, babyYHeadOffset.getFloat(model) / 16.0f, babyZHeadOffset.getFloat(model) / 16.0f);
        } catch (ReflectiveOperationException ignored) {
            // drawn where a grown-up's head would be
        }
    }

    private static Field field(String name) throws NoSuchFieldException {
        Field field = AgeableListModel.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
