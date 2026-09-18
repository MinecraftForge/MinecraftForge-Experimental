/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.RecordEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

/**
 * Fired when an input is detected from the user's input devices.
 * See the various subclasses to listen for specific devices and inputs.
 *
 * @see InputEvent.MouseButton
 * @see MouseScrollingEvent
 * @see Key
 * @see InteractionKeyMappingTriggered
 */
public sealed interface InputEvent {
    /**
     * Fired when a mouse button is pressed/released. Sub-events get fired {@link Pre before} and {@link Post after} this happens.
     *
     * <p>These events are fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     *
     * @see <a href="https://www.glfw.org/docs/latest/input_guide.html#input_mouse_button" target="_top">the online GLFW documentation</a>
     * @see Pre
     * @see Post
     */
    sealed interface MouseButton extends InputEvent {
        /**
         * {@return the MouseButtonInfo object containing information about the mouse button state}
         * @see MouseButtonInfo
         */
        MouseButtonInfo getInfo();

        /**
         * {@return the mouse button's input code}
         *
         * @see GLFW mouse constants starting with 'GLFW_MOUSE_BUTTON_'
         * @see <a href="https://www.glfw.org/docs/latest/group__buttons.html" target="_top">the online GLFW documentation</a>
         */
        default int getButton() {
            return getInfo().button();
        }

        /**
         * {@return the mouse button's action}
         *
         * @see InputConstants#PRESS
         * @see InputConstants#RELEASE
         */
        int getAction();

        /**
         * {@return a bit field representing the active modifier keys}
         *
         * @see InputConstants#MOD_CONTROL CTRL modifier key bit
         * @see GLFW#GLFW_MOD_SHIFT SHIFT modifier key bit
         * @see GLFW#GLFW_MOD_ALT ALT modifier key bit
         * @see GLFW#GLFW_MOD_SUPER SUPER modifier key bit
         * @see GLFW#GLFW_KEY_CAPS_LOCK CAPS LOCK modifier key bit
         * @see GLFW#GLFW_KEY_NUM_LOCK NUM LOCK modifier key bit
         * @see <a href="https://www.glfw.org/docs/latest/group__mods.html" target="_top">the online GLFW documentation</a>
         */
        default int getModifiers() {
            return getInfo().modifiers();
        }

        /**
         * Fired when a mouse button is pressed/released, <b>before</b> being processed by vanilla.
         *
         * <p>If the event is cancelled, then the mouse event will not be processed by vanilla (e.g. keymappings and screens) </p>
         *
         * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
         *
         * @see <a href="https://www.glfw.org/docs/latest/input_guide.html#input_mouse_button" target="_top">the online GLFW documentation</a>
         */
        record Pre(MouseButtonInfo getInfo, int getAction) implements Cancellable, MouseButton, RecordEvent {
            public static final CancellableEventBus<Pre> BUS = CancellableEventBus.create(Pre.class);

            @ApiStatus.Internal
            public Pre {}
        }

        /**
         * Fired when a mouse button is pressed/released, <b>after</b> processing.
         *
         * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
         *
         * @see <a href="https://www.glfw.org/docs/latest/input_guide.html#input_mouse_button" target="_top">the online GLFW documentation</a>
         */
        record Post(MouseButtonInfo getInfo, int getAction) implements MouseButton, RecordEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);

            @ApiStatus.Internal
            public Post {}
        }
    }

    /**
     * Fired when a mouse scroll wheel is used outside of a screen and a player is loaded, <b>before</b> being
     * processed by vanilla.
     *
     * <p>If the event is cancelled, then the mouse scroll event will not be processed further.</p>
     *
     * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     *
     * @see <a href="https://www.glfw.org/docs/latest/input_guide.html#input_mouse_button" target="_top">the online GLFW documentation</a>
     *
     * @param getDeltaX the amount of change / delta of the mouse scroll in the vertical direction
     * @param getDeltaY the amount of change / delta of the mouse scroll in the horizontal direction
     * @param isLeftDown {@code true} if the left mouse button is pressed
     * @param isMiddleDown {@code true} if the middle mouse button is pressed
     * @param isRightDown {@code true} if the right mouse button is pressed
     * @param getMouseX the X position of the mouse cursor
     * @param getMouseY the Y position of the mouse cursor
     */
    @NullMarked
    record MouseScrollingEvent(
            double getDeltaX,
            double getDeltaY,
            boolean isLeftDown,
            boolean isMiddleDown,
            boolean isRightDown,
            double getMouseX,
            double getMouseY
    ) implements Cancellable, InputEvent, RecordEvent {
        public static final CancellableEventBus<MouseScrollingEvent> BUS = CancellableEventBus.create(MouseScrollingEvent.class);

        @ApiStatus.Internal
        public MouseScrollingEvent {}
    }

    /**
     * Fired when a keyboard key input occurs, such as pressing, releasing, or repeating a key.
     *
     * <p>This event is fired only on the {@linkplain LogicalSide#CLIENT logical client}.</p>
     */
    record Key(KeyEvent getInfo, int getAction) implements RecordEvent, InputEvent {
        public static final EventBus<Key> BUS = EventBus.create(Key.class);

        @ApiStatus.Internal
        public Key {}

        /**
         * {@return the KeyEvent object containing information about the key being pressed/released}
         * @see KeyEvent
         */
        public KeyEvent getInfo() {
            return getInfo;
        }

        /**
         * {@return the {@code GLFW} (platform-agnostic) key code}
         *
         * @see InputConstants input constants starting with {@code KEY_}
         * @see GLFW key constants starting with {@code GLFW_KEY_}
         * @see <a href="https://www.glfw.org/docs/latest/group__keys.html" target="_top">the online GLFW documentation</a>
         */
        public int getKey() {
            return getInfo().key();
        }

        /**
         * {@return the mouse button's action}
         *
         * @see InputConstants#PRESS
         * @see InputConstants#RELEASE
         * @see InputConstants#REPEAT
         */
        public int getAction() {
            return getAction;
        }

        /**
         * {@return a bit field representing the active modifier keys}
         *
         * @see InputConstants#MOD_CONTROL CTRL modifier key bit
         * @see GLFW#GLFW_MOD_SHIFT SHIFT modifier key bit
         * @see GLFW#GLFW_MOD_ALT ALT modifier key bit
         * @see GLFW#GLFW_MOD_SUPER SUPER modifier key bit
         * @see GLFW#GLFW_KEY_CAPS_LOCK CAPS LOCK modifier key bit
         * @see GLFW#GLFW_KEY_NUM_LOCK NUM LOCK modifier key bit
         * @see <a href="https://www.glfw.org/docs/latest/group__mods.html" target="_top">the online GLFW documentation</a>
         */
        public int getModifiers() {
            return getInfo().modifiers();
        }
    }

    /// Fired when a keymapping that by default involves clicking the mouse buttons is triggered.
    ///
    /// The key bindings that trigger this event are:
    /// - **Use Item** - defaults to _left mouse click_
    /// - **Pick Block** - defaults to _middle mouse click_
    /// - **Attack** - defaults to _right mouse click_
    ///
    /// If this event is cancelled, then the keymapping's action is not processed further, and the hand will be swung
    /// according to [#shouldSwingHand()].
    ///
    /// This event is fired only on the [logical client][LogicalSide#CLIENT].
    ///
    /// @see InteractionKeyMappingTriggered.Attack
    /// @see InteractionKeyMappingTriggered.UseItem
    /// @see InteractionKeyMappingTriggered.PickBlock
    sealed abstract class InteractionKeyMappingTriggered extends MutableEvent implements Cancellable, InputEvent {
        private final KeyMapping keyMapping;
        private final InteractionHand hand;
        private boolean handSwing = true;

        @ApiStatus.Internal
        protected InteractionKeyMappingTriggered(KeyMapping keyMapping, InteractionHand hand) {
            this.keyMapping = keyMapping;
            this.hand = hand;
            super();
        }

        /**
         * Sets whether to swing the hand. This takes effect whether or not the event is cancelled.
         *
         * @param value whether to swing the hand
         */
        public void setSwingHand(boolean value) {
            handSwing = value;
        }

        /**
         * {@return whether to swing the hand; always takes effect, regardless of cancellation}
         */
        public boolean shouldSwingHand() {
            return handSwing;
        }

        /**
         * {@return the hand that caused the input}
         * <p>
         * The event will be called for both hands if this is a use item input regardless
         * of both event's cancellation.
         * Will always be {@link InteractionHand#MAIN_HAND} if this is an attack or pick block input.
         */
        public InteractionHand getHand() {
            return hand;
        }

        /**
         * {@return the key mapping which triggered this event}
         */
        public KeyMapping getKeyMapping() {
            return keyMapping;
        }

        /// The mouse button or input for attacking, typically the left mouse button
        public static final class Attack extends InteractionKeyMappingTriggered {
            public static final CancellableEventBus<Attack> BUS = CancellableEventBus.create(Attack.class);

            @ApiStatus.Internal
            public Attack(KeyMapping keyMapping, InteractionHand hand) {
                super(keyMapping, hand);
            }
        }

        /// The mouse button or input for using an item, typically the right mouse button
        public static final class UseItem extends InteractionKeyMappingTriggered {
            public static final CancellableEventBus<UseItem> BUS = CancellableEventBus.create(UseItem.class);

            @ApiStatus.Internal
            public UseItem(KeyMapping keyMapping, InteractionHand hand) {
                super(keyMapping, hand);
            }
        }

        /// The mouse button or input for picking a block, typically the middle mouse button (scroll wheel click)
        public static final class PickBlock extends InteractionKeyMappingTriggered {
            public static final CancellableEventBus<PickBlock> BUS = CancellableEventBus.create(PickBlock.class);

            @ApiStatus.Internal
            public PickBlock(KeyMapping keyMapping, InteractionHand hand) {
                super(keyMapping, hand);
            }

            /// @return Always false, as picking a block does not swing the hand and this event doesn't support changing
            ///         that behaviour
            @Override
            public boolean shouldSwingHand() {
                return false;
            }

            @Override
            public void setSwingHand(boolean value) {
                throw new UnsupportedOperationException("Cannot change hand swing behaviour for pick block input");
            }
        }
    }
}
