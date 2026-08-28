import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";
import { SidebarTriggerFull } from "@/components/ui/SidebarTriggerFull";

function OutletNav({ className, children, ...props }: ComponentProps<"header">) {
  return (
    <header
      className={cn(
        "absolute overflow-hidden top-0 left-0 right-0 z-40! flex items-center justify-between px-2 lg:px-4 h-12 lg:h-14 gap-2 lg:gap-4 bg-background/80 backdrop-blur-[6px] border-b",
        className
      )}
      {...props}>
      {children}
    </header>
  );
}

function OutletNavStatic({ className, children, ...props }: ComponentProps<"header">) {
  return (
    <header
      className={cn(
        "overflow-hidden w-full z-40! flex items-center justify-between px-2 lg:px-4 h-12 lg:h-14 gap-2 lg:gap-4 bg-background/80 backdrop-blur-[6px] border-b",
        className
      )}
      {...props}>
      {children}
    </header>
  );
}

function OutletNavSticky({ className, children, ...props }: ComponentProps<"header">) {
  return (
    <header
      className={cn(
        "sticky overflow-hidden w-full top-0 left-0 right-0 z-40! flex items-center justify-between px-2 lg:px-4 h-12 lg:h-14 gap-2 lg:gap-4 bg-background/80 backdrop-blur-[6px] border-b max-w-full!",
        className
      )}
      {...props}>
      {children}
    </header>
  )
}

function OutletNavContent({ className, children, ...props }: ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "relative min-w-0 flex-1",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

function OutletNavContentFlex({ className, children, ...props }: ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "text-sm font-semibold px-1 flex flex-row flex-nowrap gap-2 lg:gap-2.5 items-center",
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
}

function OutletNavVerticalSpace({ className, ...props }: ComponentProps<"div">) {
  return <div className={cn("px-2 lg:px-4 h-12 lg:h-14 w-full", className)} {...props} />
}

function OutletNavSidebarTrigger({ withSeparator, className, ...props }: ComponentProps<"div"> & { withSeparator?: boolean }) {
  return (
    <>
      <div
        className={cn(
          "shrink-0 h-12 lg:h-14 w-12 lg:w-14 -mx-2 lg:-mx-4",
          className
        )}
        {...props}>
        <SidebarTriggerFull />
      </div>
      {withSeparator && <OutletNavSeparator />}
    </>
  );
}

function OutletNavRightButton({ className, children, ...props }: ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "flex shrink-0 min-w-7 justify-center",
        className
      )}
      {...props}>
      {children}
    </div>
  );
}

function OutletNavSeparator({ className, ...props }: ComponentProps<"span">) {
  return <span className={cn("w-px bg-border h-full shrink-0 z-30!", className)} {...props}></span>;
}

function SidebarShell({ className, children, ...props }: ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "relative h-full min-h-0 flex flex-col w-full",
        className
      )}
      {...props}>
      {children}
    </div>
  );
}
function SidebarShellContent({ className, children, ...props }: ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "flex flex-col relative w-full min-w-0 min-h-0 overflow-hidden flex-1",
        className
      )}
      {...props}>
      {children}
    </div>
  );
}

export {
  OutletNav,
  OutletNavContent,
  OutletNavContentFlex,
  OutletNavSidebarTrigger,
  OutletNavRightButton,
  OutletNavSeparator,
  OutletNavVerticalSpace,
  OutletNavStatic,
  OutletNavSticky,
  SidebarShell,
  SidebarShellContent
}