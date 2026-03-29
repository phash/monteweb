# API Endpoints Reference

Base: `/api/v1/`. Format: `ResponseEntity<ApiResponse<T>>`. Auth: `Authorization: Bearer <JWT>`. Pagination: `?page=0&size=20&sort=createdAt,desc`.

## Auth
`/api/v1/auth`: register, login, logout, refresh, password-reset, password-reset/confirm, oidc/config, oidc/token

## Users
`/api/v1/users`: /me (GET/PUT), /me/avatar, /me/data-export, DELETE /me (DSGVO), /{id}, /search

## Admin
`/api/v1/admin/users`: CRUD, roles, status, csv-import | `/api/v1/admin`: config, theme, modules, logo, audit-log, error-reports, search/reindex

## Families
`/api/v1/families`: CRUD, /mine, /join, invite, children, hours, invitations

## Rooms
`/api/v1/rooms`: /mine, /browse, /discover, CRUD, settings, avatar, archive, members, mute, join-requests

## Feed
`/api/v1/feed`: feed, banners, posts CRUD, pin, comments, attachments (upload/download/delete)

## Calendar
`/api/v1/calendar`: events CRUD, cancel, rsvp, room events

## Messaging
`/api/v1/messages`: conversations, messages (multipart with images), reply threading, image download/thumbnail, WS `/ws/messages`

## Files
`/api/v1/rooms/{id}/files`: upload/download/delete, folders

## Billing
`/api/v1/billing`: periods, report (Jahresabrechnung)

## Jobboard
`/api/v1/jobs`: CRUD, apply, assignments, family hours, report/export/pdf

## Cleaning
`/api/v1/cleaning`: slots, register, swap, checkin/checkout, configs, generate, qr-codes, dashboard

## Forms
`/api/v1/forms`: CRUD, publish, close, respond, results, csv/pdf export

## Fotobox
`/api/v1/rooms/{id}/fotobox` + `/api/v1/fotobox`: threads, images, thumbnails (`?token=` JWT)

## Error Reports
`/api/v1/error-reports`: submit (public) | `/api/v1/admin/error-reports`: list, update status

## Section Admin
`/api/v1/section-admin`: rooms, members, overview for SECTION_ADMIN role

## Fundgrube
`/api/v1/fundgrube`: items CRUD, claim, images upload/download/thumbnail (`?token=` JWT)

## Bookmarks
`/api/v1/bookmarks`: CRUD bookmarks for posts, events, jobs, wiki pages

## Tasks
`/api/v1/rooms/{id}/tasks`: kanban boards, columns, tasks CRUD, drag & drop reorder

## Wiki
`/api/v1/rooms/{id}/wiki`: pages CRUD, versions, hierarchy, search

## Profile Fields
`/api/v1/profile-fields`: list, /me (GET/PUT) | `/api/v1/admin/profile-fields`: CRUD field definitions

## Search
`/api/v1/search`: global search (q, type, limit) | `/api/v1/admin/search`: reindex (Solr)

## Notifications
`/api/v1/notifications`: list, unread-count, read, read-all, delete, push subscribe/unsubscribe
