CREATE TABLE `ink_canvas_state` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`canvas_id` text NOT NULL,
	`strokes_json` text DEFAULT '[]' NOT NULL,
	`updated_at` integer NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX `ink_canvas_state_canvas_id_unique` ON `ink_canvas_state` (`canvas_id`);