package com.imgltd.mmpay.iam;

import java.util.ArrayList;
import java.util.List;

public record UserPatchRequest(String role, String password) {
  public List<String> fields() {
    var fields = new ArrayList<String>();
    if (role != null) fields.add("role");
    if (password != null) fields.add("password");
    return fields;
  }
}
