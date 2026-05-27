package com.obe.platform.security;

public final class RbacAuthority {

    private RbacAuthority() {}

    public static final String ADMIN = "ADMIN";
    public static final String ACADEMIC = "ACADEMIC";
    public static final String DIRECTOR = "DIRECTOR";
    public static final String TEACHER = "TEACHER";

    public static boolean isAdmin(String roleCode) {
        return ADMIN.equals(roleCode);
    }

    public static boolean isAcademic(String roleCode) {
        return ACADEMIC.equals(roleCode);
    }

    public static boolean isDirector(String roleCode) {
        return DIRECTOR.equals(roleCode);
    }

    public static boolean isTeacher(String roleCode) {
        return TEACHER.equals(roleCode);
    }
}
