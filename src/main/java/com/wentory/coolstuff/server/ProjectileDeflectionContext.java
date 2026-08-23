package com.wentory.coolstuff.server;

public final class ProjectileDeflectionContext {
    private static final ThreadLocal<Boolean> PROJECTILE_COLLISION =
            ThreadLocal.withInitial(() -> false);

    private ProjectileDeflectionContext() {
    }

    public static boolean isProjectileCollision() {
        return PROJECTILE_COLLISION.get();
    }

    public static void enterProjectileCollision() {
        PROJECTILE_COLLISION.set(true);
    }

    public static void leaveProjectileCollision() {
        PROJECTILE_COLLISION.remove();
    }
}