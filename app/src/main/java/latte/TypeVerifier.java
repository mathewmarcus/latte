package latte;

import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import com.google.common.collect.ImmutableSet;

public class TypeVerifier extends SimpleVerifier {
    private final static Set<Type> integerTypes = ImmutableSet.of(Type.BOOLEAN_TYPE, Type.BYTE_TYPE, Type.CHAR_TYPE, Type.INT_TYPE);

    public TypeVerifier() {
      super(/* latest api = */ ASM9, null, null, null, false);
    }

    public boolean isAssignableFrom(String currentTypeDesc, BasicValue newValue) {
        Type currentType = Type.getType(currentTypeDesc);
        if (integerTypes.contains(currentType) || integerTypes.contains(newValue.getType())) {
            return integerTypes.contains(currentType) && integerTypes.contains(newValue.getType());
        }
        BasicValue currentValue = new BasicValue(currentType);
        try {
            if (currentType.equals(SimpleVerifier.NULL_TYPE)) {
                return this.isSubTypeOf(currentValue, newValue);
            }
            return this.isSubTypeOf(newValue, currentValue);
        }
        catch (TypeNotPresentException e) {
            System.err.println(e.toString());
            return false;
        }
    }
}
