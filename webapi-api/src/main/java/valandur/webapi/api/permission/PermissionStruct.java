package valandur.webapi.api.permission;


import valandur.webapi.api.util.TreeNode;

public class PermissionStruct {

    private TreeNode<String, Boolean> permissions;
    public TreeNode<String, Boolean> getPermissions() {
        return permissions;
    }

    private int rateLimit;
    public int getRateLimit() {
        return rateLimit;
    }

    public PermissionStruct(TreeNode<String, Boolean> permissions, int rateLimit) {
        this.permissions = permissions;
        this.rateLimit = rateLimit;
    }
}