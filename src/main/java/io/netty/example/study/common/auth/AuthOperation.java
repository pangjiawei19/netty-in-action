package io.netty.example.study.common.auth;


import io.netty.example.study.common.Operation;
import lombok.Data;
import lombok.extern.java.Log;

@Data
@Log
public class AuthOperation extends Operation {

    private final String userName;
    private final String password;

    @Override
    public AuthOperationResult execute() {
        if ("admin".equalsIgnoreCase(this.userName)) {
            AuthOperationResult authOperationResult = new AuthOperationResult(true);
            return authOperationResult;
        }

        return new AuthOperationResult(false);
    }
}
