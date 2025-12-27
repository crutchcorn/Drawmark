import { eq } from 'drizzle-orm';
import { DB } from '../constants/db';
import { inkCanvasStateTable } from '../db/schema';

export interface InkCanvasState {
  id: number;
  canvasId: string;
  strokesJson: string;
  textFieldsJson: string;
  updatedAt: Date;
}

/**
 * Retrieves the ink canvas state for a given canvas ID.
 */
export async function getInkCanvasState(
  db: DB,
  canvasId: string,
): Promise<InkCanvasState | null> {
  const results = await db
    .select()
    .from(inkCanvasStateTable)
    .where(eq(inkCanvasStateTable.canvasId, canvasId))
    .limit(1);

  if (results.length === 0) {
    return null;
  }

  return results[0];
}

/**
 * Saves the ink canvas state for a given canvas ID.
 * Creates a new record if it doesn't exist, or updates the existing one.
 */
export async function saveInkCanvasState(
  db: DB,
  canvasId: string,
  strokesJson: string,
  textFieldsJson?: string,
): Promise<void> {
  const existing = await getInkCanvasState(db, canvasId);

  if (existing) {
    const updateData: {
      strokesJson: string;
      textFieldsJson?: string;
      updatedAt: Date;
    } = {
      strokesJson,
      updatedAt: new Date(),
    };
    if (textFieldsJson !== undefined) {
      updateData.textFieldsJson = textFieldsJson;
    }
    await db
      .update(inkCanvasStateTable)
      .set(updateData)
      .where(eq(inkCanvasStateTable.canvasId, canvasId));
  } else {
    await db.insert(inkCanvasStateTable).values({
      canvasId,
      strokesJson,
      textFieldsJson: textFieldsJson ?? '[]',
      updatedAt: new Date(),
    });
  }
}

/**
 * Saves only the text fields for a given canvas ID.
 */
export async function saveTextFieldsState(
  db: DB,
  canvasId: string,
  textFieldsJson: string,
): Promise<void> {
  const existing = await getInkCanvasState(db, canvasId);

  if (existing) {
    await db
      .update(inkCanvasStateTable)
      .set({
        textFieldsJson,
        updatedAt: new Date(),
      })
      .where(eq(inkCanvasStateTable.canvasId, canvasId));
  } else {
    await db.insert(inkCanvasStateTable).values({
      canvasId,
      strokesJson: '[]',
      textFieldsJson,
      updatedAt: new Date(),
    });
  }
}

/**
 * Deletes the ink canvas state for a given canvas ID.
 */
export async function deleteInkCanvasState(
  db: DB,
  canvasId: string,
): Promise<void> {
  await db
    .delete(inkCanvasStateTable)
    .where(eq(inkCanvasStateTable.canvasId, canvasId));
}
