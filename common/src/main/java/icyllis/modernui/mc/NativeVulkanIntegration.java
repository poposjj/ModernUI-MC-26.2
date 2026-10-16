/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.engine.Swizzle;
import icyllis.arc3d.vulkan.VKUtil;
import icyllis.arc3d.vulkan.VulkanBackendContext;
import icyllis.arc3d.vulkan.VulkanImage;
import icyllis.arc3d.vulkan.VulkanImageDesc;
import icyllis.arc3d.vulkan.VulkanMemoryAllocator;
import icyllis.modernui.core.VulkanManager;
import org.jetbrains.annotations.ApiStatus;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.LongBuffer;

@ApiStatus.Internal
public final class NativeVulkanIntegration {

    private NativeVulkanIntegration() {
    }

    /**
     * Resolved lazily and never throws from a static initializer: a missing or renamed
     * Minecraft field must surface as a diagnosable {@link IllegalStateException} on the
     * Vulkan code path, not as an {@code ExceptionInInitializerError} while the class is
     * being loaded (which would take down mixin application for the whole game).
     */
    private static final class Offsets {

        static final Unsafe UNSAFE;
        static final Field BACKEND_FIELD;

        static final long GPU_TEXTURE_FORMAT;
        static final long GPU_TEXTURE_WIDTH;
        static final long GPU_TEXTURE_HEIGHT;
        static final long GPU_TEXTURE_DEPTH_OR_LAYERS;
        static final long GPU_TEXTURE_MIP_LEVELS;
        static final long GPU_TEXTURE_USAGE;
        static final long GPU_TEXTURE_LABEL;

        static final long VULKAN_TEXTURE_DEVICE;
        static final long VULKAN_TEXTURE_IMAGE;
        static final long VULKAN_TEXTURE_ALLOCATION;
        static final long VULKAN_TEXTURE_CLOSED;
        static final long VULKAN_TEXTURE_VIEWS;

        static final long VULKAN_VIEW_IMAGE_VIEW;

        static final Throwable ERROR;

        static {
            Unsafe unsafe = null;
            Field backendField = null;
            long gpuTextureFormat = 0;
            long gpuTextureWidth = 0;
            long gpuTextureHeight = 0;
            long gpuTextureDepthOrLayers = 0;
            long gpuTextureMipLevels = 0;
            long gpuTextureUsage = 0;
            long gpuTextureLabel = 0;
            long vulkanTextureDevice = 0;
            long vulkanTextureImage = 0;
            long vulkanTextureAllocation = 0;
            long vulkanTextureClosed = 0;
            long vulkanTextureViews = 0;
            long vulkanViewImageView = 0;
            Throwable error = null;
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                unsafe = (Unsafe) theUnsafe.get(null);

                backendField = GpuDevice.class.getDeclaredField("backend");
                backendField.setAccessible(true);

                gpuTextureFormat = offset(unsafe, GpuTexture.class, "format");
                gpuTextureWidth = offset(unsafe, GpuTexture.class, "width");
                gpuTextureHeight = offset(unsafe, GpuTexture.class, "height");
                gpuTextureDepthOrLayers = offset(unsafe, GpuTexture.class, "depthOrLayers");
                gpuTextureMipLevels = offset(unsafe, GpuTexture.class, "mipLevels");
                gpuTextureUsage = offset(unsafe, GpuTexture.class, "usage");
                gpuTextureLabel = offset(unsafe, GpuTexture.class, "label");

                vulkanTextureDevice = offset(unsafe, VulkanGpuTexture.class, "device");
                vulkanTextureImage = offset(unsafe, VulkanGpuTexture.class, "vkImage");
                vulkanTextureAllocation = offset(unsafe, VulkanGpuTexture.class, "vmaAllocation");
                vulkanTextureClosed = offset(unsafe, VulkanGpuTexture.class, "closed");
                vulkanTextureViews = offset(unsafe, VulkanGpuTexture.class, "views");

                vulkanViewImageView = offset(unsafe, VulkanGpuTextureView.class, "vkImageView");
            } catch (Throwable t) {
                error = t;
            }
            UNSAFE = unsafe;
            BACKEND_FIELD = backendField;
            GPU_TEXTURE_FORMAT = gpuTextureFormat;
            GPU_TEXTURE_WIDTH = gpuTextureWidth;
            GPU_TEXTURE_HEIGHT = gpuTextureHeight;
            GPU_TEXTURE_DEPTH_OR_LAYERS = gpuTextureDepthOrLayers;
            GPU_TEXTURE_MIP_LEVELS = gpuTextureMipLevels;
            GPU_TEXTURE_USAGE = gpuTextureUsage;
            GPU_TEXTURE_LABEL = gpuTextureLabel;
            VULKAN_TEXTURE_DEVICE = vulkanTextureDevice;
            VULKAN_TEXTURE_IMAGE = vulkanTextureImage;
            VULKAN_TEXTURE_ALLOCATION = vulkanTextureAllocation;
            VULKAN_TEXTURE_CLOSED = vulkanTextureClosed;
            VULKAN_TEXTURE_VIEWS = vulkanTextureViews;
            VULKAN_VIEW_IMAGE_VIEW = vulkanViewImageView;
            ERROR = error;
        }
    }

    /**
     * The physical-device feature structure is referenced by every Arc3D Vulkan backend
     * context, so it must outlive them. It is therefore created once per GPU device and
     * intentionally never freed: {@code VulkanDevice} exposes no {@code isClosed()} that
     * would prove an older device is gone, and freeing early would be a use-after-free.
     */
    private static VulkanDevice sFeaturesDevice;
    private static VkPhysicalDeviceFeatures2 sFeatures;

    private static long offset(Unsafe unsafe, Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return unsafe.objectFieldOffset(field);
    }

    private static void requireAvailable() {
        Throwable error = Offsets.ERROR;
        if (error != null) {
            throw new IllegalStateException(
                    "ModernUI native Vulkan integration is unavailable on this Minecraft version", error);
        }
    }

    private static VulkanDevice device() {
        requireAvailable();
        try {
            Object backend = Offsets.BACKEND_FIELD.get(RenderSystem.getDevice());
            if (backend instanceof VulkanDevice device) {
                return device;
            }
            throw new IllegalStateException("Minecraft is not using the native Vulkan backend");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to access Minecraft GPU backend", e);
        }
    }

    public static VulkanBackendContext wrapContext() {
        VulkanDevice device = device();
        var vkDevice = device.vkDevice();
        var physicalDevice = vkDevice.getPhysicalDevice();
        var instance = physicalDevice.getInstance();

        VkPhysicalDeviceFeatures2 features2;
        synchronized (NativeVulkanIntegration.class) {
            if (sFeatures == null || sFeaturesDevice != device) {
                features2 = VkPhysicalDeviceFeatures2.calloc().sType$Default();
                sFeatures = features2;
                sFeaturesDevice = device;
            } else {
                features2 = sFeatures;
            }
        }
        VK11.vkGetPhysicalDeviceFeatures2(physicalDevice, features2);

        int apiVersion;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceProperties(physicalDevice, properties);
            apiVersion = properties.apiVersion();
        }

        VulkanMemoryAllocator allocator = VulkanMemoryAllocator.make(
                instance, physicalDevice, vkDevice, apiVersion, 0);
        if (allocator == null) {
            throw new IllegalStateException("Failed to create Arc3D Vulkan memory allocator");
        }

        VulkanManager vulkanManager = VulkanManager.get();
        vulkanManager.setPhysicalDeviceFeatures2(features2);
        vulkanManager.setMemoryAllocator(allocator);

        VulkanBackendContext backendContext = new VulkanBackendContext();
        backendContext.mInstance = instance;
        backendContext.mPhysicalDevice = physicalDevice;
        backendContext.mDevice = vkDevice;
        backendContext.mQueue = device.graphicsQueue().vkQueue();
        backendContext.mGraphicsQueueIndex = device.graphicsQueue().queueFamilyIndex();
        backendContext.mMaxAPIVersion = apiVersion;
        backendContext.mDeviceFeatures2 = features2;
        backendContext.mMemoryAllocator = allocator;
        return backendContext;
    }

    public static void replaceMainImageViewWithSwizzle(GpuTextureView textureView, short swizzle) {
        requireAvailable();
        if (!(textureView instanceof VulkanGpuTextureView vulkanView)) {
            throw new IllegalArgumentException("Expected native Vulkan texture view");
        }
        VulkanDevice device = device();
        VulkanGpuTexture texture = vulkanView.texture();
        long newView = createImageView(
                device,
                texture.vkImage(),
                texture.getFormat(),
                texture.usage(),
                texture.getDepthOrLayers(),
                vulkanView.baseMipLevel(),
                vulkanView.mipLevels(),
                swizzle
        );
        VK12.vkDestroyImageView(device.vkDevice(), vulkanView.vkImageView(), null);
        Offsets.UNSAFE.putLong(vulkanView, Offsets.VULKAN_VIEW_IMAGE_VIEW, newView);
    }

    private static long createImageView(VulkanDevice device, long image, GpuFormat format, int usage,
                                        int depthOrLayers, int baseMipLevel, int mipLevels, short swizzle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default()
                    .image(image)
                    .viewType((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0
                            ? VK10.VK_IMAGE_VIEW_TYPE_CUBE
                            : VK10.VK_IMAGE_VIEW_TYPE_2D)
                    .format(VulkanConst.toVk(format));
            createInfo.components().set(
                    VKUtil.toVkComponentSwizzle(Swizzle.getR(swizzle)),
                    VKUtil.toVkComponentSwizzle(Swizzle.getG(swizzle)),
                    VKUtil.toVkComponentSwizzle(Swizzle.getB(swizzle)),
                    VKUtil.toVkComponentSwizzle(Swizzle.getA(swizzle))
            );
            createInfo.subresourceRange()
                    .aspectMask(VulkanConst.formatAspectMask(format))
                    .baseMipLevel(baseMipLevel)
                    .levelCount(mipLevels)
                    .baseArrayLayer(0)
                    .layerCount((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0 ? 6 : depthOrLayers);

            LongBuffer pView = stack.mallocLong(1);
            VKUtil._CHECK_(VK12.vkCreateImageView(device.vkDevice(), createInfo, null, pView));
            return pView.get(0);
        }
    }

    // caller must track Arc3D CommandBuffer usage and Client usage ref
    // caller must NOT close the returned object
    public static GpuTexture wrapTextureImageFromArc3D(@RawPtr VulkanImage arc3dVulkanImage) {
        requireAvailable();
        VulkanDevice device = device();
        VulkanImageDesc desc = arc3dVulkanImage.getVulkanDesc();
        GpuFormat format = gpuFormatFromVk(desc.mVkFormat);
        try {
            VulkanGpuTexture texture = (VulkanGpuTexture) Offsets.UNSAFE.allocateInstance(VulkanGpuTexture.class);
            Offsets.UNSAFE.putObject(texture, Offsets.GPU_TEXTURE_FORMAT, format);
            Offsets.UNSAFE.putInt(texture, Offsets.GPU_TEXTURE_WIDTH, desc.getWidth());
            Offsets.UNSAFE.putInt(texture, Offsets.GPU_TEXTURE_HEIGHT, desc.getHeight());
            Offsets.UNSAFE.putInt(texture, Offsets.GPU_TEXTURE_DEPTH_OR_LAYERS, Math.max(1, desc.getLayerCount()));
            Offsets.UNSAFE.putInt(texture, Offsets.GPU_TEXTURE_MIP_LEVELS, Math.max(1, desc.getMipLevelCount()));
            Offsets.UNSAFE.putInt(texture, Offsets.GPU_TEXTURE_USAGE, GpuTexture.USAGE_TEXTURE_BINDING);
            Offsets.UNSAFE.putObject(texture, Offsets.GPU_TEXTURE_LABEL, arc3dVulkanImage.getLabel());

            Offsets.UNSAFE.putObject(texture, Offsets.VULKAN_TEXTURE_DEVICE, device);
            Offsets.UNSAFE.putLong(texture, Offsets.VULKAN_TEXTURE_IMAGE, arc3dVulkanImage.vkImage());
            Offsets.UNSAFE.putLong(texture, Offsets.VULKAN_TEXTURE_ALLOCATION, 0L);
            Offsets.UNSAFE.putBoolean(texture, Offsets.VULKAN_TEXTURE_CLOSED, false);
            Offsets.UNSAFE.putInt(texture, Offsets.VULKAN_TEXTURE_VIEWS, 0);
            return texture;
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to wrap Arc3D Vulkan image", e);
        }
    }

    private static GpuFormat gpuFormatFromVk(int vkFormat) {
        for (GpuFormat format : GpuFormat.values()) {
            if (VulkanConst.toVk(format) == vkFormat) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unsupported Vulkan image format: " + vkFormat);
    }

    public static void syncImageLayoutFromArc3D(GpuTexture vulkanTexture, @RawPtr VulkanImage arc3dVulkanImage) {
        int oldLayout = arc3dVulkanImage.getVulkanMutableState().getImageLayout();
        int newLayout = VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        if (oldLayout == newLayout) {
            return;
        }
        transitionImageLayout((VulkanGpuTexture) vulkanTexture, oldLayout, newLayout);
        arc3dVulkanImage.getVulkanMutableState().setImageLayout(newLayout);
    }

    public static void syncImageLayoutFromVulkan(GpuTexture vulkanTexture, @RawPtr VulkanImage arc3dVulkanImage) {
        arc3dVulkanImage.getVulkanMutableState().setImageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    }

    private static void transitionImageLayout(VulkanGpuTexture texture, int oldLayout, int newLayout) {
        VulkanDevice device = device();
        VulkanCommandEncoder encoder = device.createCommandEncoder();
        var commandBuffer = encoder.allocateAndBeginTransientCommandBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType$Default()
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcAccessMask(oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED
                            ? 0
                            : VK10.VK_ACCESS_MEMORY_WRITE_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT)
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(texture.vkImage());
            barrier.subresourceRange()
                    .aspectMask(VulkanConst.formatAspectMask(texture.getFormat()))
                    .baseMipLevel(0)
                    .levelCount(texture.getMipLevels())
                    .baseArrayLayer(0)
                    .layerCount(texture.getDepthOrLayers());
            VK12.vkCmdPipelineBarrier(
                    commandBuffer,
                    VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    0,
                    null,
                    null,
                    barrier
            );
        }
        VKUtil._CHECK_(VK12.vkEndCommandBuffer(commandBuffer));
        encoder.execute(commandBuffer);
    }

    public static boolean sameImage(GpuTexture vulkanTexture, @RawPtr VulkanImage arc3dVulkanImage) {
        return ((VulkanGpuTexture) vulkanTexture).vkImage() == arc3dVulkanImage.vkImage();
    }

    public static void addFrameOp(Runnable runnable) {
        device().createCommandEncoder().queueForDestroy(runnable::run);
    }
}
