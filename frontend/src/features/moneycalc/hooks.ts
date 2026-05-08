import { useMutation } from "@tanstack/react-query";
import { simulateMoneyCalc, type MoneyCalcRequest } from "./api";

export function useMoneyCalcSimulation() {
  return useMutation({
    mutationFn: (payload: MoneyCalcRequest) => simulateMoneyCalc(payload),
  });
}
