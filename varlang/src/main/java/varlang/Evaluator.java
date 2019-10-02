package varlang;
import static varlang.AST.*;
import static varlang.Value.*;

import java.util.List;
import java.util.ArrayList;

import varlang.AST.AddExp;
import varlang.AST.NumExp;
import varlang.AST.DivExp;
import varlang.AST.MultExp;
import varlang.AST.Program;
import varlang.AST.SubExp;
import varlang.AST.VarExp;
import varlang.AST.Visitor;
import varlang.Env.EmptyEnv;
import varlang.Env.ExtendEnv;

public class Evaluator implements Visitor<Value> {

	Value valueOf(Program p) {
		Env env = new EmptyEnv();
		// Value of a program in this language is the value of the expression
		return (Value) p.accept(this, env);
	}

	@Override
	public Value visit(AddExp e, Env env) {
		List<Exp> operands = e.all();
		double result = 0;
		for (Exp exp : operands) {
			NumVal intermediate = (NumVal) exp.accept(this, env); // Dynamic type-checking
			result += intermediate.v(); //Semantics of AddExp in terms of the target language.
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(NumExp e, Env env) {
		return new NumVal(e.v());
	}

	@Override
	public Value visit(DivExp e, Env env) {
		List<Exp> operands = e.all();
		NumVal lVal = (NumVal) operands.get(0).accept(this, env);
		double result = lVal.v();
		for (int i = 1; i < operands.size(); i++) {
			NumVal rVal = (NumVal) operands.get(i).accept(this, env);
			result = result / rVal.v();
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(MultExp e, Env env) {
		List<Exp> operands = e.all();
		double result = 1;
		for (Exp exp : operands) {
			NumVal intermediate = (NumVal) exp.accept(this, env); // Dynamic type-checking
			result *= intermediate.v(); //Semantics of MultExp.
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(Program p, Env env) {
		return (Value) p.e().accept(this, env);
	}

	@Override
	public Value visit(SubExp e, Env env) {
		List<Exp> operands = e.all();
		NumVal lVal = (NumVal) operands.get(0).accept(this, env);
		double result = lVal.v();
		for (int i = 1; i < operands.size(); i++) {
			NumVal rVal = (NumVal) operands.get(i).accept(this, env);
			result = result - rVal.v();
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(VarExp e, Env env) {
		// Previously, all variables had value 42. New semantics.
		return env.get(e.name());
	}

	@Override
	public Value visit(LetExp e, Env env) { // New for VarLang.
		List<String> names = e.names();
		List<Exp> value_exps = e.value_exps();
		List<Value> values = new ArrayList<Value>(value_exps.size());
		Env new_env = env;
		int counter = 0;
		/**
		 * Algorithm for expanded functionality as defined in p4:
		 * 1: Check each expression to see if it's a value exp or a variable exp.
		 *  If value,
		 *  If it's the latter, just add to the value_exps to the list
		 */
		for (Exp exp : value_exps) {
			// if it's a value exp, this will succeed and we move on.
			// To keep track of variables, we'll also update the env here
			String var_name = "";
			try {
				VarExp varExp = (VarExp) exp.accept(this, new_env);
				var_name = varExp.name();
				Value value = new_env.get(var_name);
				new_env = new ExtendEnv(new_env, names.get(counter), value);

			}
			catch(Exception e1) {
				values.add((Value) exp.accept(this, new_env));
				new_env = new ExtendEnv(new_env, names.get(counter), values.get(counter));
			}

			// If the above block fails, we've found a var exp. To add it, we need to know what it means, man.
			// So we need the value. Since we modified this method to update env in this for loop,
			// we can use the get method to find the value. We then again update the env
			counter++;
		}
		// return the result of the final computation
		return (Value) e.body().accept(this, new_env);
	}
}