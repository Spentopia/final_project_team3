-- 월간 성실도 점수 테이블
create extension if not exists pgcrypto;

create table if not exists public.monthly_scores (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  month_start date not null,
  record_days_score integer not null default 0 check (record_days_score between 0 and 30),
  receipt_score integer not null default 0 check (receipt_score between 0 and 25),
  diary_score integer not null default 0 check (diary_score between 0 and 20),
  budget_score integer not null default 0 check (budget_score between 0 and 15),
  streak_score integer not null default 0 check (streak_score between 0 and 10),
  total_score integer not null default 0 check (total_score between 0 and 100),
  reward_granted boolean not null default false,
  finalized_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  constraint monthly_scores_month_start_is_first_day
    check (extract(day from month_start) = 1),
  constraint monthly_scores_user_month_unique
    unique (user_id, month_start)
);

create index if not exists monthly_scores_user_month_desc_idx
  on public.monthly_scores (user_id, month_start desc);

create index if not exists monthly_scores_finalize_idx
  on public.monthly_scores (month_start, total_score)
  where reward_granted = false;

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger monthly_scores_set_updated_at
before update on public.monthly_scores
for each row
execute function public.set_updated_at();

alter table public.monthly_scores enable row level security;

create policy "monthly_scores_select_own"
on public.monthly_scores
for select
to authenticated
using (auth.uid() = user_id);

-- 클라이언트가 점수를 직접 만들거나 고치면 보상 기준을 조작할 수 있으므로
-- insert/update/delete 정책은 만들지 않는다. 백엔드는 service_role 키로 RLS를 우회한다.
