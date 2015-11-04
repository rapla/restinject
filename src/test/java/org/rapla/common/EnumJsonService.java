package org.rapla.common;

import org.rapla.jsonrpc.common.RemoteJsonMethod;
import org.rapla.jsonrpc.common.FutureResult;

@RemoteJsonMethod
public interface EnumJsonService
{

    public enum TrueFalse
    {
        TRUE, FALSE
    }

    public class Parameter
    {
        private TrueFalse selection;
        private String reason;
    }

    FutureResult<TrueFalse> insert(String comment);

    TrueFalse get(Parameter param);

}
