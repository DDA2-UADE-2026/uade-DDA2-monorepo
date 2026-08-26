import { useSyncExternalStore } from "react"

import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  getAvatarForUser,
  getAvatarUserLabel,
  readCurrentUserAvatar,
  subscribeCurrentUserAvatar,
  type AvatarUser,
} from "@/lib/avatar"
import { getUserInitials } from "@/lib/user-display"

type UserAvatarProps = Omit<React.ComponentProps<typeof Avatar>, "children"> & {
  user: AvatarUser
  fallbackClassName?: string
  addBlob?: boolean
}

function UserAvatar({ user, fallbackClassName, addBlob, ...props }: UserAvatarProps) {
  const stored = useSyncExternalStore(
    subscribeCurrentUserAvatar,
    readCurrentUserAvatar,
    () => null,
  )
  const label = getAvatarUserLabel(user)

  if (addBlob) {
    const avatarSrc = getAvatarForUser(user, stored)
    return (
      <>
        <Avatar aria-label={label} {...props}>
          <AvatarImage src={avatarSrc} alt={label} className={"z-10!"} />
          <AvatarFallback className={fallbackClassName}>
            {getUserInitials(label)}
          </AvatarFallback>
          <img
            src={avatarSrc}
            alt={label}
            className="absolute inset-0 h-full w-full rounded-full object-cover blur-md opacity-60 z-0"
            aria-hidden="true"
          />
        </Avatar>
      </>
    );
  }

  return (
    <Avatar aria-label={label} {...props}>
      <AvatarImage src={getAvatarForUser(user, stored)} alt={label} />
      <AvatarFallback className={fallbackClassName}>
        {getUserInitials(label)}
      </AvatarFallback>
    </Avatar>
  )
}

export { UserAvatar }
