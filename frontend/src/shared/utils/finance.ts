import { Transaction } from "@/shared/providers/FinanceProvider";

export const getMonthlyExpenseTotal = (
  transactions: Transaction[],
  baseDate: Date
) => {
  return transactions
    .filter((t) => {
      const date = new Date(t.date);
      return (
        t.type === "expense" &&
        date.getFullYear() === baseDate.getFullYear() &&
        date.getMonth() === baseDate.getMonth()
      );
    })
    .reduce((sum, t) => sum + t.amount, 0);
};

export const getMonthlyIncomeTotal = (
  transactions: Transaction[],
  baseDate: Date
) => {
  return transactions
    .filter((t) => {
      const date = new Date(t.date);
      return (
        t.type === "income" &&
        date.getFullYear() === baseDate.getFullYear() &&
        date.getMonth() === baseDate.getMonth()
      );
    })
    .reduce((sum, t) => sum + t.amount, 0);
};