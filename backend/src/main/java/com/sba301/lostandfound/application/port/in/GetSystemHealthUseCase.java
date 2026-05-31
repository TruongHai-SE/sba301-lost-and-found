package com.sba301.lostandfound.application.port.in;

import com.sba301.lostandfound.application.result.SystemHealthResult;

public interface GetSystemHealthUseCase {

    SystemHealthResult getHealth();
}
