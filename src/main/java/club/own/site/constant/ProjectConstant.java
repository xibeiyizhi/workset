package club.own.site.constant;

public class ProjectConstant {

    public enum MemberEnum {
        FATHER,
        MOTHER,
        DAUGHTER,
        SON;
    }

    public static final String MEMBER_ADD_APPLY_KEY = "member_add_apply";
    public static final int MEMBER_ADD_APPLY_EXPIRE = 604800 ; // 7天

    public static final String MEMBER_LIST_KEY = "member_list";
    public static final String MEMBER_TAGS_KEY = "member_tag";
    public static final String BLOG_LIST_KEY = "blog_list";
}
