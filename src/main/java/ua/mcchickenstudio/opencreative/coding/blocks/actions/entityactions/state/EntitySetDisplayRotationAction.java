/*
 * OpenCreative+, Minecraft plugin.
 * (C) 2022-2026, McChicken Studio, mcchickenstudio@gmail.com
 *
 * OpenCreative+ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenCreative+ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ua.mcchickenstudio.opencreative.coding.blocks.actions.entityactions.state;

import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import ua.mcchickenstudio.opencreative.coding.arguments.Arguments;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.ActionType;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.Target;
import ua.mcchickenstudio.opencreative.coding.blocks.actions.entityactions.EntityAction;
import ua.mcchickenstudio.opencreative.coding.blocks.executors.Executor;
import ua.mcchickenstudio.opencreative.coding.exceptions.UnsupportedEntityException;

public final class EntitySetDisplayRotationAction extends EntityAction {
    public EntitySetDisplayRotationAction(Executor executor, Target target, int x, Arguments args) {
        super(executor, target, x, args);
    }

    @Override
    public void executeEntity(@NotNull Entity entity) {
        if (!(entity instanceof Display display)) {
            throw new UnsupportedEntityException(Display.class, entity);
        }
        boolean add = getArguments().getBoolean("add", false, this);
        boolean side = getArguments().getBoolean("side", false, this);
        float x = 0;
        float y = 0;
        float z = 0;
        if (getArguments().pathExists("x")) {
            x = (float) Math.toRadians(getArguments().getFloat("x", x, this))/2;
        }
        if (getArguments().pathExists("y")) {
            y = (float) Math.toRadians(getArguments().getFloat("y", y, this))/2;
        }
        if (getArguments().pathExists("z")) {
            z = (float) Math.toRadians(getArguments().getFloat("z", z, this))/2;
        }
        Quaternionf oldRotation = side ? display.getTransformation().getRightRotation() : display.getTransformation().getLeftRotation();
        Quaternionf quaternionf = add ? oldRotation : new Quaternionf(0,0,0,1).rotationXYZ(x,y,z);
        quaternionf = quaternionf.rotateXYZ(x,y,z);
        display.setTransformation(new Transformation(
                display.getTransformation().getTranslation(),
                (side ? display.getTransformation().getLeftRotation() : quaternionf),
                display.getTransformation().getScale(),
                (side ? quaternionf : display.getTransformation().getRightRotation())
        ));
    }

    @Override
    public @NotNull ActionType getActionType() {
        return ActionType.ENTITY_SET_DISPLAY_ROTATION;
    }
}
