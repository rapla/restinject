package org.rapla.gwtjsonrpc.annotation;

import org.rapla.gwtjsonrpc.RemoteJsonMethod;
import org.rapla.gwtjsonrpc.common.FutureResult;

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
